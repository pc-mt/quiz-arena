package com.example.http;

import io.vertx.ext.web.Router;

public interface HttpController {
    // Registriert alle HTTP-Routen des jeweiligen Controllers.
    public void registerRoutes(Router router);
}
