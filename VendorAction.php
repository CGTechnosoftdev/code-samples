<?php

namespace App;

use Illuminate\Database\Eloquent\Model;
use OwenIt\Auditing\Contracts\Auditable;

/**
 * Class VendorAction
 *
 * Represents an Vendor Action entity in the system.
 * Stores details about Vendor actions, their related files, 
 *
 * Implements auditing to track changes using 
 * the OwenIt\Auditing package.
 */
class VendorAction extends Model implements Auditable
{
    // Enable auditing for this model
    use \OwenIt\Auditing\Auditable;

    /**
     * The database table associated with the model.
     *
     * @var string
     */
    protected $table = 'vendor_actions';

    /**
     * The attributes that aren't mass assignable.
     * 
     * An empty array means all attributes are mass assignable.
     *
     * @var array
     */
    protected $guarded = [];

    /**
     * Relationship: VendorAction belongs to a User.
     * 
     * - Each Vendor action is created/owned by a user.
     * - Uses soft-deleted users as well (withTrashed).
     *
     * @return \Illuminate\Database\Eloquent\Relations\BelongsTo
     */
    public function user()
    {
        return $this->belongsTo(User::class, 'user_id')->withTrashed();
    }

    /**
     * Relationship: VendorAction may have multiple Files.
     * 
     * - Polymorphic many-to-many relation.
     * - Allows attaching files (e.g., documents, PDFs) 
     *   to Vendor actions.
     *
     * @return \Illuminate\Database\Eloquent\Relations\MorphToMany
     */
    public function files()
    {
        return $this->morphToMany(File::class, 'fileable');
    }

    /**
     * Relationship: VendorAction has many VendorActionClasses.
     * 
     * - One-to-many relation.
     * - Defines the classes associated with this Vendor action.
     *
     * @return \Illuminate\Database\Eloquent\Relations\HasMany
     */
    public function classes()
    {
        return $this->hasMany(VendorActionClass::class, 'vendor_action_id');
    }
}
