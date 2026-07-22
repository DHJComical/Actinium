package org.taumc.celeritas.compat;

final class InstallOnce {
    private boolean installed;

    synchronized boolean run(Runnable installer) {
        if (installed) {
            return false;
        }
        installer.run();
        installed = true;
        return true;
    }
}
