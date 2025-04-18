package it.noi.edisplay.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for standardizing room name handling
 */
public class RoomNameUtils {
    
    private static final Logger logger = LoggerFactory.getLogger(RoomNameUtils.class);
    
    /**
     * Normalizes room names for consistent comparison
     * 
     * @param roomName The room name to normalize
     * @return Normalized room name
     */
    public static String normalizeRoomName(String roomName) {
        if (roomName == null) {
            return "";
        }
        
        // Remove any leading/trailing whitespace
        String normalized = roomName.trim();
        
        // Convert to uppercase for case-insensitive comparison
        normalized = normalized.toUpperCase();
        
        // Remove any building prefix variations (A, B, C) if they exist
        normalized = normalized.replaceAll("^(BUILDING|BLD|BLDG)\\s*[A-C]\\s*-\\s*", "");
        
        // Remove any floor designations
        normalized = normalized.replaceAll("\\s*FLOOR\\s*\\d+\\s*", " ");
        
        // Remove any trailing room word variations
        normalized = normalized.replaceAll("\\s*(ROOM|RM)$", "");
        
        // Replace multiple spaces with a single space
        normalized = normalized.replaceAll("\\s+", " ");
        
        logger.debug("Normalized room name '{}' to '{}'", roomName, normalized);
        return normalized;
    }
}