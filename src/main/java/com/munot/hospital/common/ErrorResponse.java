package com.munot.hospital.common;

import java.time.LocalDateTime;
import java.util.Map;

public record ErrorResponse(LocalDateTime timestamp, int status, String message, Map<String, String> fieldErrors) {}
