<?php

namespace MarioDevv\HealthConnect;

use Illuminate\Support\ServiceProvider;

class HealthConnectServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(HealthConnect::class);
    }
}
