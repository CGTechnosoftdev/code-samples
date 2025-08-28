<?php

namespace App;

use Illuminate\Database\Eloquent\Model;
use Illuminate\Database\Eloquent\SoftDeletes;

class Domain extends Model
{
    use SoftDeletes;

    const Active = 'Active';
    const Blocked = 'Blocked';

    protected $guarded = [];
}
