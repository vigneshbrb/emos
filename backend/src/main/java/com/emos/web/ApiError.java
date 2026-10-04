package com.emos.web;

public record ApiError(String type, String title, int status, String detail) { }
