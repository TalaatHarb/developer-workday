package net.talaatharb.workday.ui.startup;

import lombok.extern.slf4j.Slf4j;

/**
 * Windows startup manager.
 */
@Slf4j
public class WindowsStartupManager {
    public boolean enableStartup() {
        log.info("Enabling Windows startup (stub)");
        return true;
    }
    
    public boolean disableStartup() {
        return true;
    }
    
    public boolean isStartupEnabled() {
        return false;
    }
}
