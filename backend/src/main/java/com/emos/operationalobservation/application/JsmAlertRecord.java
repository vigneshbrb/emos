package com.emos.operationalobservation.application;

import tools.jackson.databind.JsonNode;

public record JsmAlertRecord(JsmAlertSnapshot snapshot, JsonNode rawPayload) {}
