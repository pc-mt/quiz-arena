package com.example.object;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.http.HttpController;

import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;

public class ObjectController implements HttpController {

    private static final Logger logger = LoggerFactory.getLogger(ObjectController.class);
    private final ObjectService objectService;
    private final EventBus eventBus;

    public ObjectController(Vertx vertx) {
        this.eventBus = vertx.eventBus();
        this.objectService = new ObjectService();
    }

    public void registerRoutes(Router router) {
        // --- Objekt-Routen ---
        router.post("/api/objects").handler(this::handleCreate);
        router.get("/api/objects").handler(this::handleRead);
        router.put("/api/objects/:id").handler(this::handleUpdate);
        router.delete("/api/objects/:id").handler(this::handleDelete);
    }

    // Objekt anlegen
    private void handleCreate(RoutingContext ctx) {
        String name = ctx.body().asString();

        logger.info("ctx.body().asString(): {}", name);

        objectService.createObject(name, res -> {
            if (res.succeeded()) {
                ctx.response().setStatusCode(201).end("Object created and saved in DB.");
                logger.debug("Created object in database");

                // Event fuer nachgelagerte MQTT-Verarbeitung senden.
                eventBus.publish("object.created", name);

                logger.info("Event published: object.created with name: {}", name);
            } else {
                ctx.response()
                        .setStatusCode(500)
                        .end();
                logger.error("Failed to create object in database");
            }
        });

    }

    // Objektliste laden
    private void handleRead(RoutingContext ctx) {
        // Daten ueber Service laden.
        objectService.readObjects(res -> {
            if (res.succeeded()) {
                ctx.response()
                        .putHeader("content-type", "application/json")
                        .end(res.result().encode());
                logger.debug("Fetched objects from database");
            } else {
                ctx.response().setStatusCode(500).end("Failed to fetch data from database");
                logger.error("Failed to fetch data from database: {}", res.cause().getMessage());
            }
        });
    }

    // Objekt per ID aktualisieren
    private void handleUpdate(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        String message = ctx.body().asString();
        logger.debug("Received request to update object with id: {} and message: {}", id, message);

        // Update ueber Service ausfuehren.
        objectService.updateObject(id, message, res -> {
            if (res.succeeded()) {
                ctx.response().setStatusCode(200).end("Object updated successfully");
                logger.debug("Updated object with id: {}", id);
            } else {
                ctx.response().setStatusCode(500).end("Failed to update data in database");
                logger.error("Failed to update data in database: {}", res.cause().getMessage());
            }
        });
    }

    // Objekt per ID loeschen
    private void handleDelete(RoutingContext ctx) {
        int id = Integer.parseInt(ctx.pathParam("id"));
        logger.debug("Received request to delete object with id: {}", id);

        // Loeschen ueber Service ausfuehren.
        objectService.deleteObject(id, res -> {
            if (res.succeeded()) {
                ctx.response().setStatusCode(200).end("Object deleted successfully");
                logger.debug("Deleted object with id: {}", id);
            } else {
                ctx.response().setStatusCode(500).end("Failed to delete data from database");
                logger.error("Failed to delete data from database: {}", res.cause().getMessage());
            }
        });
    }

}
