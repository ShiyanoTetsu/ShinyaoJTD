package com.shiyano.shinyaoJTD;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** Определяет, где лежит папка content после установки/в dev-режиме. */
public final class AppPaths {
    private AppPaths() {}

    /** Корень установки (для jpackage это либо сам каталог с .exe, либо папка выше /bin). */
    public static Path installBaseDir() {
        String appPath = System.getProperty("jpackage.app-path");
        if (appPath != null && !appPath.isBlank()) {
            Path exe = Paths.get(appPath);
            Path parent = exe.getParent();                 // C:\Program Files\JPTrainer   (installer)
            if (parent != null && parent.getFileName() != null &&
                    parent.getFileName().toString().equalsIgnoreCase("bin")) {
                // app-image вариант: ...\JPTrainer\bin\JPTrainer.exe → base = ...\JPTrainer
                Path up = parent.getParent();
                return up != null ? up : parent;
            }
            return parent != null ? parent : exe.toAbsolutePath().getParent();
        }
        try {
            var uri = AppPaths.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            Path loc = Paths.get(uri).toAbsolutePath();
            Path parent = loc.getParent();
            return parent != null ? parent : Paths.get("").toAbsolutePath();
        } catch (URISyntaxException e) {
            return Paths.get("").toAbsolutePath();
        }
    }

    public static Path contentDir() {
        Path base = installBaseDir();
        Path c1 = base.resolve("content");
        if (Files.isDirectory(c1)) return c1;

        Path c2 = base.resolve("app").resolve("content");
        if (Files.isDirectory(c2)) return c2;

        // fallback: в dev-режиме рядом с проектом
        return Paths.get("content").toAbsolutePath();
    }
}
