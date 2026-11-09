package com.examprep.security;

import java.util.UUID;

/**
 * Single source of the current student's identifier for all business services.
 */
public interface CurrentUserService {

    UUID requireCurrentUserId();
}
