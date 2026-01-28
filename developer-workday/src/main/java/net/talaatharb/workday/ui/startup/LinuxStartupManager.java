package net.talaatharb.workday.ui.startup;

import lombok.extern.slf4j.Slf4j;

/**
 * Linux startup manager.
 */
@Slf4j
public class LinuxStartupManager {
    public boolean enableStartup() {
        log.info("Enabling Linux startup (stub)");
        return true;
    }
    
    public boolean disableStartup() {
        return true;
    }
    
    public boolean isStartupEnabled() {
        return false;
    }
}
