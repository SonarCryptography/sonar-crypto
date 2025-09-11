#!/usr/bin/env powershell

<#
.SYNOPSIS
    Development script for SonarCrypto Plugin - Install and Manage

.DESCRIPTION
    This script provides easy commands to install the SonarCrypto plugin into a local 
    SonarQube instance for development and testing purposes. The install command automatically
    handles building, packaging, and installation.

.PARAMETER Command
    The command to execute: install, clean, or help

.PARAMETER SonarHome
    Path to your SonarQube installation directory (required for install command)

.EXAMPLE
    .\dev-script.ps1 install -SonarHome "C:\sonarqube-10.0"
    .\dev-script.ps1 clean
    .\dev-script.ps1 help
#>

param(
    [Parameter(Mandatory=$true)]
    [ValidateSet("install", "clean", "help")]
    [string]$Command,
    
    [Parameter(Mandatory=$false)]
    [string]$SonarHome = ""
)

# Set error action preference
$ErrorActionPreference = "Stop"

# Project configuration
$PROJECT_ROOT = $PSScriptRoot
$PLUGIN_MODULE = "sonar-crypto-plugin"
$PLUGIN_JAR_PATTERN = "sonar-crypto-plugin-*.jar"

function Write-Header {
    param([string]$Title)
    Write-Host "`n===================================================" -ForegroundColor Cyan
    Write-Host "  $Title" -ForegroundColor Cyan
    Write-Host "===================================================" -ForegroundColor Cyan
}

function Write-Success {
    param([string]$Message)
    Write-Host "[SUCCESS] $Message" -ForegroundColor Green
}

function Write-Error {
    param([string]$Message)
    Write-Host "[ERROR] $Message" -ForegroundColor Red
}

function Write-Info {
    param([string]$Message)
    Write-Host "[INFO] $Message" -ForegroundColor Yellow
}

function Test-Maven {
    try {
        $mvnVersion = mvn -version 2>$null
        if ($LASTEXITCODE -eq 0) {
            Write-Success "Maven is available"
            return $true
        }
    }
    catch {
        Write-Error "Maven is not available in PATH. Please install Maven first."
        return $false
    }
    return $false
}

function Build-Plugin {
    Write-Header "Building SonarCrypto Plugin"
    
    if (-not (Test-Maven)) {
        exit 1
    }
    
    Write-Info "Cleaning and compiling the project..."
    Set-Location $PROJECT_ROOT
    
    try {
        mvn clean compile -q
        Write-Success "Build completed successfully"
    }
    catch {
        Write-Error "Build failed: $($_.Exception.Message)"
        exit 1
    }
}

function Package-Plugin {
    Write-Header "Packaging SonarCrypto Plugin"
    
    if (-not (Test-Maven)) {
        exit 1
    }
    
    Write-Info "Creating plugin package..."
    Set-Location $PROJECT_ROOT
    
    try {
        mvn clean package -q
        
        # Find the generated JAR file
        $jarFile = Get-ChildItem -Path "$PROJECT_ROOT\$PLUGIN_MODULE\target" -Name $PLUGIN_JAR_PATTERN | Select-Object -First 1
        
        if ($jarFile) {
            $jarPath = "$PROJECT_ROOT\$PLUGIN_MODULE\target\$jarFile"
            Write-Success "Plugin packaged successfully: $jarFile"
            Write-Info "JAR location: $jarPath"
            Write-Info "JAR size: $([math]::Round((Get-Item $jarPath).Length / 1MB, 2)) MB"
            return $jarPath
        }
        else {
            Write-Error "Plugin JAR file not found in target directory"
            exit 1
        }
    }
    catch {
        Write-Error "Packaging failed: $($_.Exception.Message)"
        exit 1
    }
}

function Install-Plugin {
    param([string]$SonarHomePath)
    
    Write-Header "Installing SonarCrypto Plugin to SonarQube"
    
    if ([string]::IsNullOrEmpty($SonarHomePath)) {
        Write-Error "SonarQube home path is required for installation"
        Write-Info "Usage: .\dev-script.ps1 install -SonarHome 'C:\path\to\sonarqube'"
        exit 1
    }
    
    if (-not (Test-Path $SonarHomePath)) {
        Write-Error "SonarQube directory not found: $SonarHomePath"
        exit 1
    }
    
    $pluginsDir = Join-Path $SonarHomePath "extensions\plugins"
    if (-not (Test-Path $pluginsDir)) {
        Write-Error "SonarQube plugins directory not found: $pluginsDir"
        Write-Info "Make sure the provided path is a valid SonarQube installation"
        exit 1
    }
    
    # First, package the plugin
    $jarPath = Package-Plugin
    
    # Remove existing plugin versions
    Write-Info "Removing existing SonarCrypto plugin versions..."
    $existingPlugins = Get-ChildItem -Path $pluginsDir -Name "sonar-crypto-plugin-*.jar" -ErrorAction SilentlyContinue
    foreach ($plugin in $existingPlugins) {
        $pluginPath = Join-Path $pluginsDir $plugin
        Remove-Item $pluginPath -Force
        Write-Success "Removed existing plugin: $plugin"
    }
    
    # Copy new plugin
    Write-Info "Installing new plugin version..."
    $jarFileName = Split-Path $jarPath -Leaf
    $destinationPath = Join-Path $pluginsDir $jarFileName
    Copy-Item $jarPath $destinationPath -Force
    Write-Success "Plugin installed: $jarFileName"
    
    Write-Info ""
    Write-Info "Installation completed! Next steps:"
    Write-Info "1. Restart your SonarQube server"
    Write-Info "2. Check the logs for any startup issues"
    Write-Info "3. Verify the plugin is loaded in SonarQube Administration > System"
    Write-Info ""
    Write-Info "Plugin location: $destinationPath"
}

function Clean-Project {
    Write-Header "Cleaning Project"
    
    if (-not (Test-Maven)) {
        exit 1
    }
    
    Write-Info "Cleaning build artifacts..."
    Set-Location $PROJECT_ROOT
    
    try {
        mvn clean -q
        Write-Success "Project cleaned successfully"
    }
    catch {
        Write-Error "Clean failed: $($_.Exception.Message)"
        exit 1
    }
}

function Show-Help {
    Write-Header "SonarCrypto Plugin Development Script"
    
    Write-Host ""
    Write-Host "This script helps you install the SonarCrypto plugin for development." -ForegroundColor White
    Write-Host ""
    Write-Host "COMMANDS:" -ForegroundColor Yellow
    Write-Host "  install  - Build, package, and install plugin to SonarQube (requires -SonarHome)" -ForegroundColor White
    Write-Host "  clean    - Clean all build artifacts" -ForegroundColor White
    Write-Host "  help     - Show this help message" -ForegroundColor White
    Write-Host ""
    Write-Host "EXAMPLES:" -ForegroundColor Yellow
    Write-Host "  .\dev-script.ps1 install -SonarHome 'C:\sonarqube-10.0'" -ForegroundColor Gray
    Write-Host "  .\dev-script.ps1 clean" -ForegroundColor Gray
    Write-Host ""
    Write-Host "NOTE:" -ForegroundColor Yellow
    Write-Host "  For individual Maven operations, use Maven directly:" -ForegroundColor White
    Write-Host "  mvn clean compile    # Build only" -ForegroundColor Gray
    Write-Host "  mvn clean package    # Package only" -ForegroundColor Gray
    Write-Host ""
    Write-Host "REQUIREMENTS:" -ForegroundColor Yellow
    Write-Host "  - Apache Maven (mvn command available in PATH)" -ForegroundColor White
    Write-Host "  - Java 17 or later" -ForegroundColor White
    Write-Host "  - Internet connection (for downloading CrySL rules during build)" -ForegroundColor White
    Write-Host ""
    Write-Host "PLUGIN INFO:" -ForegroundColor Yellow
    Write-Host "  Plugin Key: crypto" -ForegroundColor White
    Write-Host "  Plugin Name: Cryptography Analysis" -ForegroundColor White
    Write-Host "  Description: Detect Cryptographical Issues" -ForegroundColor White
    Write-Host ""
}

# Main script execution
try {
    switch ($Command.ToLower()) {
        "install" { 
            Install-Plugin -SonarHomePath $SonarHome 
        }
        "clean" { 
            Clean-Project 
        }
        "help" { 
            Show-Help 
        }
        default { 
            Write-Error "Unknown command: $Command"
            Show-Help
            exit 1
        }
    }
}
catch {
    Write-Error "Script execution failed: $($_.Exception.Message)"
    exit 1
}

Write-Host ""
Write-Success "Script completed successfully!"