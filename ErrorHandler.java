package com.example.financial;

import javafx.scene.control.Alert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorHandler {
    private static final Logger LOGGER = LoggerFactory.getLogger(ErrorHandler.class);

    public static void handleException(Throwable e, String message, javafx.scene.Node context) {
        LOGGER.error(message, e);
        javafx.application.Platform.runLater(() -> 
            new Alert(Alert.AlertType.ERROR, message + ": " + e.getMessage()).showAndWait());
    }

    public static void validateRequiredField(String value, String fieldName) throws ValidationException {
        if (value == null || value.trim().isEmpty()) {
            throw new ValidationException(fieldName + " is required");
        }
    }

    public static boolean validateStringSafe(String value, String fieldName) {
        try {
            validateRequiredField(value, fieldName);
            return true;
        } catch (ValidationException e) {
            LOGGER.warn(e.getMessage());
            return false;
        }
    }

    public static double validatePositiveDouble(String value, String fieldName, double min, double max) throws ValidationException {
        validateRequiredField(value, fieldName);
        try {
            double result = Double.parseDouble(value);
            if (result < min || result > max) {
                throw new ValidationException(fieldName + " must be between " + min + " and " + max);
            }
            return result;
        } catch (NumberFormatException e) {
            throw new ValidationException(fieldName + " must be a valid number");
        }
    }
}