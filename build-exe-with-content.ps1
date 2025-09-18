# build-exe-with-content.ps1
# Одностраничный скрипт: build jar -> jlink runtime -> jpackage installer (включая content)

# === Настройки: проставь пути/имена при необходимости ===
$AppName    = "JPTrainer"
$Version    = "0.1.0"
$MainClass  = "com.shiyano.shinyaoJTD.Main"
$JfxJmods   = "C:\javafx-jmods-21.0.8"   # где распакованы javafx jmods
$ResourceDir= "pack"                    # временная папка, которую добавим в resource-dir
$IconPath   = "pack\icons\app.ico"      # если нет иконки — закомментируй использование иконки ниже

# === Простейшие проверки окружения ===
if (-not (Get-Command jpackage -ErrorAction SilentlyContinue)) {
    Write-Error "jpackage не найден в PATH. Убедись, что установлен JDK 21 и jpackage доступен."
    exit 1
}
if (-not (Get-Command jlink -ErrorAction SilentlyContinue)) {
    Write-Error "jlink не найден. Убедись, что JDK установлен (jlink входит в JDK)."
    exit 1
}
if (-not (Test-Path $JfxJmods)) {
    Write-Error "Не найден путь к javafx jmods: $JfxJmods. Проверь переменную в скрипте."
    exit 1
}
if (-not (Test-Path "content")) {
    Write-Error "Не найдена папка content/ в корне проекта. Положи туда topics.json и items-*.json."
    exit 1
}

Write-Host "1) Собираем проект (Gradle -> jar)..."
.\gradlew clean build
if ($LASTEXITCODE -ne 0) { Write-Error "gradle build не удался"; exit $LASTEXITCODE }

# Подготовка resource-dir (копируем content туда)
Write-Host "2) Подготавливаем ресурсную папку ($ResourceDir)..."
Remove-Item -Recurse -Force $ResourceDir -ErrorAction SilentlyContinue
New-Item -ItemType Directory -Force -Path $ResourceDir | Out-Null
# копируем content (robocopy удобнее для больших папок)
robocopy content "$ResourceDir\content" /E /NFL /NDL /NJH /NJS /NP | Out-Null

# (опционально) можно положить иконку в pack\icons\app.ico заранее; если нет — не беда.

# Удаляем старые runtime/dist
Remove-Item -Recurse -Force runtime, dist -ErrorAction SilentlyContinue

Write-Host "3) Создаём урезанный runtime через jlink..."
# jlink использует системные jmods и javafx jmods
$jlinkModulePath = "$env:JAVA_HOME\jmods;$JfxJmods"
jlink --module-path $jlinkModulePath `
      --add-modules java.base,java.logging,javafx.base,javafx.graphics,javafx.controls `
      --strip-debug --no-header-files --no-man-pages `
      --output runtime
if ($LASTEXITCODE -ne 0) { Write-Error "jlink завершился с ошибкой"; exit $LASTEXITCODE }

Write-Host "4) Собираем инсталлятор через jpackage..."
$Jar = (Get-ChildItem build\libs -Filter *.jar | Select-Object -First 1).Name
if (-not $Jar) { Write-Error "Jar не найден в build\libs. Сначала gradle собери."; exit 1 }

$jpackageArgs = @(
  "--name", $AppName,
  "--app-version", $Version,
  "--type", "exe",
  "--dest", "dist",
  "--runtime-image", "runtime",
  "--input", "build\libs",
  "--main-jar", $Jar,
  "--main-class", $MainClass,
  "--resource-dir", $ResourceDir,
  "--win-menu",
  "--win-shortcut",
  "--java-options", "--Dfile.encoding=UTF-8 --Dconsole.encoding=UTF-8 --Dsun.stdout.encoding=UTF-8 --Dsun.stderr.encoding=UTF-8"
)

# добавим иконку, если она есть
if (Test-Path $IconPath) {
  $jpackageArgs += @("--icon", $IconPath)
}

Write-Host "Запускаем jpackage..."
jpackage @jpackageArgs
if ($LASTEXITCODE -ne 0) { Write-Error "jpackage завершился с ошибкой"; exit $LASTEXITCODE }

Write-Host ""
Write-Host "Готово! Инсталлятор находится в папке dist\"
Get-ChildItem dist -Filter *.exe | Format-List Name,Length,LastWriteTime
