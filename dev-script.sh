#!/bin/bash

# Development script for SonarCrypto Plugin - Install and Manage
# Cross-platform version for macOS and Linux
#
# This script provides easy commands to install the SonarCrypto plugin into a local 
# SonarQube instance for development and testing purposes. The install command automatically
# handles building, packaging, and installation.
#
# Usage:
#   ./dev-script.sh install [SONAR_HOME]
#   ./dev-script.sh clean
#   ./dev-script.sh help

set -e  # Exit on any error

# Project configuration
PROJECT_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PLUGIN_MODULE="sonar-crypto-plugin"
PLUGIN_JAR_PATTERN="sonar-crypto-plugin-*.jar"

# Color codes for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

function print_header() {
    local title="$1"
    echo -e "\n${CYAN}===================================================${NC}"
    echo -e "${CYAN}  $title${NC}"
    echo -e "${CYAN}===================================================${NC}"
}

function print_success() {
    local message="$1"
    echo -e "${GREEN}[SUCCESS] $message${NC}"
}

function print_error() {
    local message="$1"
    echo -e "${RED}[ERROR] $message${NC}" >&2
}

function print_info() {
    local message="$1"
    echo -e "${YELLOW}[INFO] $message${NC}"
}

function test_maven() {
    if command -v mvn &> /dev/null; then
        print_success "Maven is available"
        return 0
    else
        print_error "Maven is not available in PATH. Please install Maven first."
        return 1
    fi
}

function build_plugin() {
    print_header "Building SonarCrypto Plugin"
    
    if ! test_maven; then
        exit 1
    fi
    
    print_info "Cleaning and compiling the project..."
    cd "$PROJECT_ROOT"
    
    if mvn clean compile -q; then
        print_success "Build completed successfully"
    else
        print_error "Build failed"
        exit 1
    fi
}

function package_plugin() {
    print_header "Packaging SonarCrypto Plugin"
    
    if ! test_maven; then
        exit 1
    fi
    
    print_info "Creating plugin package..."
    cd "$PROJECT_ROOT"
    
    if mvn clean package -q; then
        # Find the generated JAR file
        local jar_file
        jar_file=$(find "$PROJECT_ROOT/$PLUGIN_MODULE/target" -name "$PLUGIN_JAR_PATTERN" -type f | head -n 1)
        
        if [[ -n "$jar_file" ]]; then
            local jar_name
            jar_name=$(basename "$jar_file")
            print_success "Plugin packaged successfully: $jar_name"
            print_info "JAR location: $jar_file"
            
            # Calculate file size in MB
            local size_mb
            if [[ "$OSTYPE" == "darwin"* ]]; then
                # macOS
                size_mb=$(stat -f%z "$jar_file" | awk '{printf "%.2f", $1/1024/1024}')
            else
                # Linux
                size_mb=$(stat -c%s "$jar_file" | awk '{printf "%.2f", $1/1024/1024}')
            fi
            print_info "JAR size: ${size_mb} MB"
            echo "$jar_file"
            return 0
        else
            print_error "Plugin JAR file not found in target directory"
            exit 1
        fi
    else
        print_error "Packaging failed"
        exit 1
    fi
}

function install_plugin() {
    local sonar_home="$1"
    
    print_header "Installing SonarCrypto Plugin to SonarQube"
    
    if [[ -z "$sonar_home" ]]; then
        print_error "SonarQube home path is required for installation"
        print_info "Usage: ./dev-script.sh install /path/to/sonarqube"
        exit 1
    fi
    
    if [[ ! -d "$sonar_home" ]]; then
        print_error "SonarQube directory not found: $sonar_home"
        exit 1
    fi
    
    local plugins_dir="$sonar_home/extensions/plugins"
    if [[ ! -d "$plugins_dir" ]]; then
        print_error "SonarQube plugins directory not found: $plugins_dir"
        print_info "Make sure the provided path is a valid SonarQube installation"
        exit 1
    fi
    
    # First, package the plugin
    local jar_path
    jar_path=$(package_plugin)
    
    # Remove existing plugin versions
    print_info "Removing existing SonarCrypto plugin versions..."
    if ls "$plugins_dir"/sonar-crypto-plugin-*.jar &> /dev/null; then
        for plugin in "$plugins_dir"/sonar-crypto-plugin-*.jar; do
            local plugin_name
            plugin_name=$(basename "$plugin")
            rm -f "$plugin"
            print_success "Removed existing plugin: $plugin_name"
        done
    fi
    
    # Copy new plugin
    print_info "Installing new plugin version..."
    local jar_filename
    jar_filename=$(basename "$jar_path")
    local destination_path="$plugins_dir/$jar_filename"
    cp "$jar_path" "$destination_path"
    print_success "Plugin installed: $jar_filename"
    
    echo ""
    print_info "Installation completed! Next steps:"
    print_info "1. Restart your SonarQube server"
    print_info "2. Check the logs for any startup issues"
    print_info "3. Verify the plugin is loaded in SonarQube Administration > System"
    echo ""
    print_info "Plugin location: $destination_path"
}

function clean_project() {
    print_header "Cleaning Project"
    
    if ! test_maven; then
        exit 1
    fi
    
    print_info "Cleaning build artifacts..."
    cd "$PROJECT_ROOT"
    
    if mvn clean -q; then
        print_success "Project cleaned successfully"
    else
        print_error "Clean failed"
        exit 1
    fi
}

function show_help() {
    print_header "SonarCrypto Plugin Development Script"
    
    echo ""
    echo "This script helps you install the SonarCrypto plugin for development."
    echo ""
    echo -e "${YELLOW}COMMANDS:${NC}"
    echo "  install  - Build, package, and install plugin to SonarQube (requires SONAR_HOME path)"
    echo "  clean    - Clean all build artifacts"
    echo "  help     - Show this help message"
    echo ""
    echo -e "${YELLOW}EXAMPLES:${NC}"
    echo -e "${NC}  ./dev-script.sh install /opt/sonarqube-10.0${NC}"
    echo -e "${NC}  ./dev-script.sh clean${NC}"
    echo ""
    echo -e "${YELLOW}NOTE:${NC}"
    echo "  For individual Maven operations, use Maven directly:"
    echo -e "${NC}  mvn clean compile    # Build only${NC}"
    echo -e "${NC}  mvn clean package    # Package only${NC}"
    echo ""
    echo -e "${YELLOW}REQUIREMENTS:${NC}"
    echo "  - Apache Maven (mvn command available in PATH)"
    echo "  - Java 17 or later"
    echo "  - Internet connection (for downloading CrySL rules during build)"
    echo ""
    echo -e "${YELLOW}PLUGIN INFO:${NC}"
    echo "  Plugin Key: crypto"
    echo "  Plugin Name: Cryptography Analysis"
    echo "  Description: Detect Cryptographical Issues"
    echo ""
}

# Main script execution
if [[ $# -eq 0 ]]; then
    show_help
    exit 1
fi

command="$1"
shift

case "$command" in
    "install")
        if [[ $# -eq 0 ]]; then
            print_error "SonarQube home path is required for install command"
            show_help
            exit 1
        fi
        install_plugin "$1"
        ;;
    "clean")
        clean_project
        ;;
    "help"|"--help"|"-h")
        show_help
        ;;
    *)
        print_error "Unknown command: $command"
        show_help
        exit 1
        ;;
esac

echo ""
print_success "Script completed successfully!"