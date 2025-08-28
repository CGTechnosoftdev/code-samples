<?php

namespace App\Http\Controllers;

use Illuminate\Http\Request;
use App\Constants\GateTypes;
use App\Domain;
use Illuminate\Support\Facades\Gate;
use Throwable;

class DomainController extends Controller
{
    /**
     * @OA\Get(
     *      path="/domains",
     *      operationId="getDomainList",
     *      tags={"Domain"},
     *      summary="Get domain list",
     *      description="",
     *      @OA\Parameter(
     *          name="page",
     *          description="page number",
     *          in="query",
     *          @OA\Schema(
     *              type="integer",
     *              default=1,
     *          ),
     *      ),
     *      @OA\Parameter(
     *          name="per_page",
     *          description="page limit",
     *          in="query",
     *          @OA\Schema(
     *              type="integer",
     *              default=10,
     *          ),
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=200,
     *          description="Successful",
     *       ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=401,
     *          description="Unauthenticated",
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=403,
     *          description="Forbidden"
     *      ),
     *       security={
     *         {
     *             "passport": {}
     *         }
     *     }
     *  )
     */
    public function index(Request $request)
    {
        try {
            if (Gate::denies(GateTypes::IS_EMPLOYEE)) {
                return response()->json(['message' => "Invalid request."], 403);
            }

            $per_page = $request->get('per_page') ?? 10;
            $keyword = $request->get('search_keyword') ?? null;

            $query =  Domain::orderBy('created_at', 'desc');
            if (!empty($keyword)) {
                $query->where('name', 'LIKE', '%' . $keyword . '%');
            }
            $domais = $query->paginate($per_page);
            return response()->json(['data' => $domais], 200);
        } catch (Throwable $e) {
            return response()->json(['message' => $e], 400);
        }
    }

    /**
     * @OA\Post(
     *      path="/domains",
     *      operationId="addDomain",
     *      tags={"Domain"},
     *      summary="Add domain",
     *      description="",
     *      @OA\RequestBody(
     *          description="",
     *          required=true,
     *          @OA\MediaType(
     *              mediaType="application/json",
     *              @OA\Schema(
     *                  type="object",
     *                  @OA\Property(
     *                      property="name",
     *                      description="",
     *                      type="string"
     *                  ),
     *                  @OA\Property(
     *                      property="status",
     *                      description="",
     *                      type="string",
     *                      enum={"Active","Blocked"}
     *                  ),
     *                  required={"name", "status"}
     *              )
     *          )
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=200,
     *          description="Successful",
     *       ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=422,
     *          description="Error",
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response="default",
     *          description="Error",
     *      ),
     *      security={
     *         {
     *             "passport": {}
     *         }
     *     }
     *   )
     */
    public function store(Request $request)
    {
        try {
            if (Gate::denies(GateTypes::IS_EMPLOYEE)) {
                return response()->json(['message' => "Invalid request."], 403);
            }

            $data = $request->validate([
                'name' => 'required|max:100|unique:domains,name,NULL,id,deleted_at,NULL',
                'status' => 'required',
                'threshold_limit' => 'required|integer|min:1|max:100',
                'exclude_for_report' => 'required',
            ]);

            $domain = new Domain([
                'name' => $data['name'],
                'status' => $data['status'],
                'threshold_limit' => $data['threshold_limit'],
                'exclude_for_report' => $data['exclude_for_report']
            ]);

            $domain->save();

            return response()->json(['data' => $domain, 'message' => "New domain added."], 201);
        } catch (Throwable $e) {
            return response()->json(['message' => $e->getMessage()], 400);
        }
    }

    /**
     * @OA\Put(
     *      path="/domains/{domain_id}",
     *      operationId="updateDomainField",
     *      tags={"Domain"},
     *      summary="Update domain field",
     *      description="",
     *      @OA\Parameter(
     *          name="domain_id",
     *          description="",
     *          required=true,
     *          in="path",
     *          @OA\Schema(
     *              type="integer",
     *          )
     *      ),
     *      @OA\RequestBody(
     *          description="",
     *          required=true,
     *          @OA\MediaType(
     *              mediaType="application/json",
     *              @OA\Schema(
     *                  type="object",
     *                  @OA\Property(
     *                      property="name",
     *                      description="",
     *                      type="string"
     *                  ),
     *                  @OA\Property(
     *                      property="status",
     *                      description="",
     *                      type="string",
     *                      enum={"Active","Blocked"}
     *                  ),
     *                  required={"name", "status"}
     *              )
     *          )
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=200,
     *          description="Successful",
     *       ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=422,
     *          description="Error",
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response="default",
     *          description="Error",
     *      ),
     *       security={
     *         {
     *             "passport": {}
     *         }
     *     }
     *  )
     */
    public function update(Request $request, Domain $domain)
    {
        try {
            if (Gate::denies(GateTypes::IS_EMPLOYEE)) {
                return response()->json(['message' => "Invalid request."], 403);
            }

            $data = $request->validate([
                'field' => 'required',
                'value' => 'required'
            ]);

            $field = $data['field'];
            $domain->$field = $data['value'];

            if ($field == "name") {
                $domain_exists = Domain::where('name', $data['value'])->where('id', '!=', $domain->id)->first();

                if ($domain_exists) {
                    return response()->json(['message' => "This domain name already exists."], 400);
                }
            }

            $domain->save();

            return response()->json(['data' => $domain, 'message' => "Domain updated successfully."], 200);
        } catch (\Illuminate\Database\QueryException $ex) {
            return response()->json(['message' => 'Error while saving the data.'], 400);
        } catch (Throwable $e) {
            return response()->json(['message' => $e], 400);
        }
    }

    /**
     * @OA\Delete(
     *      path="/domains/{domain_id}",
     *      operationId="deleteDomain",
     *      tags={"Domain"},
     *      summary="Delete domain",
     *      description="",
     *      @OA\Parameter(
     *          name="domain_id",
     *          description="",
     *          required=true,
     *          in="path",
     *          @OA\Schema(
     *              type="integer",
     *          )
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=200,
     *          description="Successful",
     *       ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=422,
     *          description="Error",
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response="default",
     *          description="Error",
     *      ),
     *       security={
     *         {
     *             "passport": {}
     *         }
     *     }
     *  )
     */
    public function destroy(Domain $domain)
    {
        try {
            if (Gate::denies(GateTypes::IS_EMPLOYEE)) abort(401);

            $domain->delete();

            return response()->json(['message' => "Domain Deleted Successfully."], 200);
        } catch (Throwable $e) {
            return response()->json(['message' => $e], 400);
        }
    }

    /**
     * @OA\Put(
     *      path="/update-domain/{domain_id}",
     *      operationId="updateDmain",
     *      tags={"Domain"},
     *      summary="Update domain details",
     *      description="",
     *      @OA\Parameter(
     *          name="domain_id",
     *          description="",
     *          required=true,
     *          in="path",
     *          @OA\Schema(
     *              type="integer",
     *          )
     *      ),
     *      @OA\RequestBody(
     *          description="",
     *          required=true,
     *          @OA\MediaType(
     *              mediaType="application/json",
     *              @OA\Schema(
     *                  type="object",
     *                  @OA\Property(
     *                      property="name",
     *                      description="",
     *                      type="string"
     *                  ),
     *                  @OA\Property(
     *                      property="status",
     *                      description="",
     *                      type="string",
     *                      enum={"Active","Blocked"}
     *                  ),
     *                  @OA\Property(
     *                      property="threshold_limit",
     *                      description="",
     *                      type="string",
     *                  ),
     *                  @OA\Property(
     *                      property="exclude_for_report",
     *                      description="",
     *                      type="string",
     *                  ),
     *                  required={"name", "status", "threshold_limit", "exclude_for_report"}
     *              )
     *          )
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=200,
     *          description="Successful",
     *       ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response=422,
     *          description="Error",
     *      ),
     *      @OA\Response(
     *          @OA\MediaType(mediaType="application/json"),
     *          response="default",
     *          description="Error",
     *      ),
     *       security={
     *         {
     *             "passport": {}
     *         }
     *     }
     *  )
     */
    public function updateDomain(Request $request, Domain $domain)
    {
        try {

            if (Gate::denies(GateTypes::IS_EMPLOYEE)) {
                return response()->json(['message' => "Invalid request."], 403);
            }

            $data = $request->validate([
                'name' => 'required|max:100|unique:domains,name,' . $domain->id . ',id,deleted_at,NULL',
                'status' => 'required',
                'threshold_limit' => 'required|integer|min:1|max:100',
                'exclude_for_report' => 'required',
            ]);

            $domain->name = $data['name'];
            $domain->status = $data['status'];
            $domain->threshold_limit = $data['threshold_limit'];
            $domain->exclude_for_report = $data['exclude_for_report'];

            $domain->save();

            return response()->json(['data' => $domain, 'message' => "Domain updated successfully."], 200);
        } catch (\Illuminate\Database\QueryException $ex) {
            return response()->json(['message' => 'Error while saving the data.'], 400);
        } catch (Throwable $e) {
            return response()->json(['message' => $e->getMessage()], 400);
        }
    }
}
