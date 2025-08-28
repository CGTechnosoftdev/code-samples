<?php

namespace App\Http\Controllers;

use App\Constants\GateTypes;
use App\Constants\HttpStatus;
use App\Services\ApiClientService;
use App\Address;
use App\AddressChangeLogEmailQueue;
use App\Traits\ApiResponse;
use App\User;
use Illuminate\Http\Request;
use Illuminate\Support\Facades\Gate;
use Illuminate\Support\Facades\Log;
use Illuminate\Support\Facades\Validator;
use Throwable;

class VendorController extends Controller
{
    use ApiResponse;

    protected $apiClient;

    public function __construct()
    {
        $this->apiClient = new ApiClientService();
    }


    /**
     * @OA\Post(
     *     path="/sync-address",
     *     operationId="syncAddress",
     *     tags={"Sample"},
     *     summary="Sync Address (API call from vendor)",
     *     description="Creates a new vendor address or updates existing records that share the same vtoken. The 'state' field is excluded during updates to preserve state-specific associations.",
     *     @OA\RequestBody(
     *         required=true,
     *         @OA\MediaType(
     *             mediaType="application/json",
     *             @OA\Schema(
     *                 type="object",
     *                 @OA\Property(property="vtoken", type="string", description="Unique identifier for address"),
     *                 @OA\Property(property="state", type="string", description="State name"),
     *                 @OA\Property(property="address", type="string", description="Address line"),
     *                 @OA\Property(property="status", type="string", description="Status of address (e.g., active/inactive)"),
     *                 @OA\Property(property="is_default", type="boolean", description="Flag if address is default"),
     *                 required={"vtoken"}
     *             )
     *         )
     *     ),
     *     @OA\Response(
     *         response=200,
     *         description="Successful",
     *         @OA\JsonContent(
     *             @OA\Property(property="success", type="boolean"),
     *             @OA\Property(property="message", type="string"),
     *             @OA\Property(property="data", type="object", example={})
     *         )
     *     ),
     *     @OA\Response(
     *         response=422,
     *         description="Validation Error",
     *         @OA\JsonContent(
     *             @OA\Property(property="success", type="boolean"),
     *             @OA\Property(property="message", type="string"),
     *             @OA\Property(property="errors", type="object")
     *         )
     *     ),
     *     @OA\Response(
     *         response=400,
     *         description="Unexpected Error"
     *     )
     * )
     */
    public function syncAddress(Request $request)
    {
        try {
            // 1. Validate input
            $validator = Validator::make($request->all(), [
                'vtoken' => 'required|string',
                'state' => 'required|string',
                'address' => 'required|string',
                'status' => 'required|in:0,1', // must use integer (0 or 1)
                'is_default' => 'required|in:0,1', // must use integer (0 or 1)
            ]);

            if ($validator->fails()) {
                Log::error("Validation failed.", ['errors' => $validator->errors()]);
                return $this->sendErrorResponse('Validation error', $validator->errors(), HttpStatus::UNPROCESSABLE_ENTITY);
            }

            // 3. Extract required request data
            $vtoken = $request->input('vtoken');

            /**
             * 4. Fetch all Address records where the vtoken matches.
             * If a state doesn't have an vendor address, the address from another state was added to it as provided.
             * As a result, multiple states can have the same address with the same vtoken.
             */
            $records = Address::where('vtoken', $vtoken)->get();

            if ($records->isNotEmpty()) {
                /**
                 * 5. Update existing records if found
                 *
                 * Exclude 'state' during update.
                 *
                 * On the vendor side, we store the same address for multiple states. This is because,
                 * if a state doesn't have an vendor address, the address from another state was added to it as provided.
                 * Meanwhile, vendor stores a single address with one state.
                 *
                 * For example, if the same address is assigned to both AL and AR states (sharing the same vtoken),
                 * and we update the record using the vtoken — including the 'state' — both rows will end up with the same state (e.g., AL),
                 * causing a mismatch in vendor s data representation.
                 */
                $data = [
                    'address' => $request->input('address'),
                    'status' => (int) $request->input('status'),
                    'is_default' => (int) $request->input('is_default'),
                ];

                $addressDataArr = [];
                foreach ($records as $record) {
                    // Prepare email queue data for change/retire vendor address notifications
                    $addressDataArr[] =  [
                        'id' => $record->id,
                        'state' => $record->state,
                        'oldAddress' => $record->address,
                        'newAddress' => $data['address'],
                        'status' => 0 // Inactive and Changed
                    ];

                    // Update the data
                    $record->update($data);
                }

                // Process all retired vendor addresses and add entries to the email queue table to notify users of address changes or retired
                foreach ($addressDataArr as $address) {
                    $this->prepareEmailQueueForRetiredAddress($address);
                }

                $message = 'Vendor address updated successfully';
                Log::info($message, ['updated_records' => $records->toArray()]);
            } else {
                // 6. Create a new record if no match found
                $data = [
                    'vtoken' => $vtoken,
                    'state' => $request->input('state'),
                    'address' => $request->input('address'),
                    'status' => (int) $request->input('status'),
                    'is_default' => (int) $request->input('is_default'),
                ];
                $newRecord = Address::create($data);
                $message = 'Vendor address created successfully';
                Log::info($message, ['created_record' => $newRecord->toArray()]);
            }

            return $this->sendSuccessResponse([], $message, HttpStatus::OK);
        } catch (Throwable $e) {
            Log::error("Exception thrown in syncAddress.", ['exception' => $e]);
            return $this->sendErrorResponse('Unexpected error occurred', $e->getMessage(), HttpStatus::BAD_REQUEST);
        }
    }

    /**
     * @OA\Post(
     *     path="/notify-retired-address-users",
     *     operationId="notifyUserOfRetiredAddress",
     *     tags={"Sample"},
     *     summary="Queue email notifications for users with retired Vendor addresses",
     *     description="Identifies users linked to retired Vendor addresses who have active premium privacy subscriptions and queues email notifications for them.",
     *     @OA\Response(
     *         response=200,
     *         description="Process completed successfully",
     *         @OA\JsonContent(
     *             @OA\Property(property="success", type="boolean", example=true),
     *             @OA\Property(property="message", type="string", example="Vendor Address Sync Completed")
     *         )
     *     ),
     *     @OA\Response(
     *         response=400,
     *         description="Unexpected Error",
     *         @OA\JsonContent(
     *             @OA\Property(property="success", type="boolean", example=false),
     *             @OA\Property(property="message", type="string"),
     *             @OA\Property(property="error", type="string")
     *         )
     *     )
     * )
     */
    public function notifyUserOfRetiredAddress(Request $request)
    {
        if (!Gate::allows(GateTypes::IS_ADMIN)) {
            return response()->json(['message' => 'Unauthorized request.'], 403);
        }

        try {
            // Step 1: Get all retired Vendor addresses
            $retiredAddresses = $this->getRetiredAddresses();

            // Step 2: Process each retired Vendor address and queue emails for eligible users and orders
            foreach ($retiredAddresses as $addressObj) {
                // Step 2a: Update old Vendor address with the new address in DB and fetch data from third-party API
                $this->updateAddress($addressObj);

                // Step 2b: Prepare email queue for users affected by the retired address
                $this->prepareEmailQueueForRetiredAddress($addressObj);
            }

            // Step 3: Return success response
            return $this->sendSuccessResponse(null, 'All records processed successfully.');
        } catch (Throwable $e) {
            Log::error('Exception in notifyUserOfRetiredAddress.', ['exception' => $e]);
            return $this->sendErrorResponse('Unexpected error occurred', $e->getMessage(), HttpStatus::BAD_REQUEST);
        }
    }

    private function updateAddress($addressObj)
    {
        try {
            $newAddress = trim($addressObj['newAddress']);
            $newAddressLower = strtolower($newAddress);

            // Fetch vtoken of the new address
            $newAddressRecord = Address::whereRaw('LOWER(TRIM(address)) = ?', [$newAddressLower])
                ->whereNotNull('vtoken')
                ->first();

            Log::info('Fetched Vendor address record for new address.', [
                'new_address' => $newAddress,
                'record' => $newAddressRecord
            ]);

            // Proceed only if record and vtoken are present
            if ($newAddressRecord && !empty($newAddressRecord->vtoken)) {

                // Fetch Vendor address data for old address id
                $AddressRecord = Address::find($addressObj['id']);

                if ($AddressRecord) {
                    // Update fields with new address data
                    $AddressRecord->update([
                        'address'     => $newAddress,
                        'vendor_id'      => $newAddressRecord->vendor_id ?? null,
                        'vtoken'        => $newAddressRecord->vtoken,
                        'status'      => $newAddressRecord->status,
                        'is_default'  => $newAddressRecord->is_default,
                    ]);

                    Log::info('Vendor address updated successfully for new address.', [
                        'new_address' => $newAddress,
                        'new_address_id' => $newAddressRecord->id,
                        'new_address_vtoken' => $newAddressRecord->vtoken
                    ]);
                } else {
                    Log::warning("No Vendor record found for old address.", [
                        'record' => $addressObj['id'],
                        'old_address' => $addressObj['oldAddress'],
                    ]);
                }
            } else {
                // Log specific cases for missing or null vtoken
                if (!$newAddressRecord) {
                    Log::error('No Vendor address record found for new address.', [
                        'new_address' => $newAddress
                    ]);
                } elseif (empty($newAddressRecord->vtoken)) {
                    Log::error('Vendor address record found but vtoken is missing for new address.', [
                        'new_address' => $newAddress,
                        'record' => $newAddressRecord->toArray()
                    ]);
                }
            }
        } catch (Throwable $e) {
            Log::error('Exception while updating Vendor address for new address.', [
                'new_address' => $newAddress,
                'exception' => $e->getMessage(),
                'trace' => $e->getTraceAsString(),
            ]);
        }
    }

    private function getRetiredAddresses()
    {
        $addressArr = [
            [
                'id' => 1,
                'state' => 'AL',
                'oldAddress' => 'old street',
                'newAddress' => 'new street',
                'status' => 1 // Retired and Changed
            ]
        ];

        return $addressArr;
    }

    private function prepareEmailQueueForRetiredAddress($address)
    {
        // Step 1: Fetch users linked to this retired Vendor address with premium privacy enabled
        $users = User::where('vendor_address_id', $address['id'])
            ->where('privacy', 2) // 2 => user opted for premium privacy
            ->get();

        if ($users->isEmpty()) {
            // Log if no users found for the retired address
            Log::info("No users found for retired Vendor address.", ['Vendor address ID' => $address['id']]);
            return; // Skip to the next address
        }

        Log::info("Users found for Vendor address", [
            'Vendor Address ID' => $address['id'],
            'Total Users Count' => $users->count(),
        ]);

        $emailDataArr = [];

        // Step 2: Process each user
        foreach ($users as $user) {
            $userId = $user->id;

            // Prepare ONE email entry per user
            $emailDataArr[$userId] = [
                'user_id' => $userId,
                'vendor_address_id' => $address['id'],
                'old_vendor_address' => $address['oldAddress'],
                'new_vendor_address' => $address['newAddress'],
                'vendor_address_status' => $address['status'],
            ];
        }

        // Step 6: Queue emails if not already logged to send notifications later
        foreach ($emailDataArr as $emailData) {
            // Skip if a record already exists for this combination
            $oldAddress = strtolower(trim($emailData['old_vendor_address']));
            $newAddress = strtolower(trim($emailData['new_vendor_address']));

            $exists = AddressChangeLogEmailQueue::where('user_id', $emailData['user_id'])
                ->where('vendor_address_id', $emailData['vendor_address_id'])
                ->whereRaw('LOWER(TRIM(old_vendor_address)) = ?', [$oldAddress])
                ->whereRaw('LOWER(TRIM(new_vendor_address)) = ?', [$newAddress])
                ->exists();

            if ($exists) {
                continue; // Skip this record
            }

            // Create new entry in email queue
            AddressChangeLogEmailQueue::create([
                'user_id' => $emailData['user_id'],
                'order_ids' => $emailData['order_ids'],
                'vendor_address_id' => $emailData['vendor_address_id'],
                'old_vendor_address' => $emailData['old_vendor_address'],
                'new_vendor_address' => $emailData['new_vendor_address'],
                'vendor_address_status' => $emailData['vendor_address_status'],
                'is_email_sent' => 0, // 0 => Not sent
            ]);
        }
    }
}
