package com.workpilot.security;

public record CurrentUser(Long id, String loginName, String displayName, String userType) {
}
