#!/usr/bin/env python3
"""
Source Code Generator for Brute Morse Code Android App
Generates a comprehensive, Claude-friendly source code listing.

Usage:
    python gen_source.py [output_file]
    
    output_file: Optional output filename (default: morse_source_YYYYMMDD_HHMMSS.txt)
"""

import os
import sys
from datetime import datetime
from pathlib import Path

# ============================================================================
# CONFIGURATION
# ============================================================================

# Project root - auto-detect or set manually
PROJECT_ROOT = Path(__file__).parent.absolute()
APP_SRC_DIR = PROJECT_ROOT / "app" / "src" / "main"
JAVA_SRC_DIR = APP_SRC_DIR / "java" / "com" / "example" / "brutemorse"
RES_DIR = APP_SRC_DIR / "res"

# File extensions to include
SOURCE_EXTENSIONS = {'.kt', '.java'}
CONFIG_EXTENSIONS = {'.xml', '.gradle', '.kts', '.properties'}
DOC_EXTENSIONS = {'.md', '.txt'}

# Directories to EXCLUDE (build artifacts, IDE files, etc.)
EXCLUDE_DIRS = {
    '.gradle',
    '.idea',
    'build',
    'gradle',  # Exclude gradle wrapper
    '.kotlin',
    'outputs',
    'intermediates',
    'tmp',
    'caches',
    '.git',
}

# Files to ALWAYS include (important config files)
IMPORTANT_FILES = {
    'AndroidManifest.xml',
    'build.gradle.kts',
    'settings.gradle.kts',
    'gradle.properties',
    'README.md',
    'proguard-rules.pro',
}

# Files to EXCLUDE
EXCLUDE_FILES = {
    'local.properties',  # Contains local paths
    'gradlew',
    'gradlew.bat',
    '.gitignore',
}

# ============================================================================
# FILE COLLECTION
# ============================================================================

def should_exclude_dir(dir_path: Path) -> bool:
    """Check if directory should be excluded."""
    dir_name = dir_path.name
    return dir_name in EXCLUDE_DIRS or dir_name.startswith('.')

def should_include_file(file_path: Path) -> bool:
    """Determine if a file should be included in the output."""
    file_name = file_path.name
    
    # Exclude specific files
    if file_name in EXCLUDE_FILES:
        return False
    
    # Always include important files
    if file_name in IMPORTANT_FILES:
        return True
    
    # Include by extension
    suffix = file_path.suffix.lower()
    return suffix in SOURCE_EXTENSIONS or suffix in CONFIG_EXTENSIONS or suffix in DOC_EXTENSIONS

def collect_files(root_dir: Path) -> dict:
    """
    Collect all relevant files organized by category.
    
    Returns:
        dict with keys: 'source', 'config', 'resources', 'docs'
    """
    files = {
        'manifest': [],
        'gradle': [],
        'source': [],
        'resources': [],
        'docs': [],
    }
    
    for file_path in root_dir.rglob('*'):
        # Skip directories
        if file_path.is_dir():
            continue
        
        # Skip excluded directories
        if any(should_exclude_dir(parent) for parent in file_path.parents):
            continue
        
        # Skip if not included
        if not should_include_file(file_path):
            continue
        
        # Categorize file
        relative_path = file_path.relative_to(root_dir)
        
        if file_path.name == 'AndroidManifest.xml':
            files['manifest'].append(relative_path)
        elif file_path.suffix in {'.gradle', '.kts', '.properties'}:
            files['gradle'].append(relative_path)
        elif file_path.suffix in SOURCE_EXTENSIONS:
            files['source'].append(relative_path)
        elif file_path.suffix == '.xml' and 'res' in file_path.parts:
            files['resources'].append(relative_path)
        elif file_path.suffix in DOC_EXTENSIONS:
            files['docs'].append(relative_path)
    
    # Sort files within each category
    for category in files:
        files[category].sort()
    
    return files

# ============================================================================
# OUTPUT GENERATION
# ============================================================================

def generate_header(output_file):
    """Generate header section for the output file."""
    timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
    
    header = f"""
{'='*80}
BRUTE MORSE CODE - ANDROID APPLICATION SOURCE CODE
{'='*80}

Generated: {timestamp}
Project: Brute Morse Code Android App
Purpose: Complete source code listing for Claude AI analysis

This file contains the complete source code of the Brute Morse Code Android
application, organized for easy review and analysis.

PROJECT STRUCTURE:
- Source Code: Kotlin files implementing app logic
- Configuration: Gradle build files, manifest
- Resources: XML layouts, strings, themes
- Documentation: README and other docs

{'='*80}

"""
    output_file.write(header)

def write_file_section(output_file, category_name: str, file_paths: list, root_dir: Path):
    """Write a section of files to the output."""
    if not file_paths:
        return
    
    output_file.write(f"\n{'='*80}\n")
    output_file.write(f"{category_name.upper()}\n")
    output_file.write(f"{'='*80}\n")
    output_file.write(f"Files in this section: {len(file_paths)}\n")
    output_file.write(f"{'='*80}\n\n")
    
    for relative_path in file_paths:
        full_path = root_dir / relative_path
        
        output_file.write(f"\n{'-'*80}\n")
        output_file.write(f"FILE: {relative_path}\n")
        output_file.write(f"{'-'*80}\n\n")
        
        try:
            with open(full_path, 'r', encoding='utf-8') as f:
                content = f.read()
                output_file.write(content)
                
                # Ensure file ends with newline
                if not content.endswith('\n'):
                    output_file.write('\n')
                    
        except Exception as e:
            output_file.write(f"[ERROR: Could not read file - {e}]\n")
        
        output_file.write(f"\n{'-'*80}\n")
        output_file.write(f"END OF FILE: {relative_path}\n")
        output_file.write(f"{'-'*80}\n\n")

def generate_summary(output_file, files: dict):
    """Generate summary at the end of the file."""
    total_files = sum(len(file_list) for file_list in files.values())
    
    summary = f"""
{'='*80}
SUMMARY
{'='*80}

Total files included: {total_files}

Breakdown by category:
- Manifest files:     {len(files['manifest'])}
- Gradle files:       {len(files['gradle'])}
- Source files:       {len(files['source'])}
- Resource files:     {len(files['resources'])}
- Documentation:      {len(files['docs'])}

{'='*80}
END OF SOURCE CODE LISTING
{'='*80}

INSTRUCTIONS FOR CLAUDE:
This is a complete source code listing of the Brute Morse Code Android app.
The app is an educational morse code training application using:
- Kotlin with Jetpack Compose
- MVVM architecture
- Custom audio generation for morse code
- Multiple learning algorithms (BCT, ARM1V, NestedID, etc.)

Key areas of focus:
1. audio/ - Morse code audio generation and playback
2. model/ - Learning algorithms and data structures
3. viewmodel/ - UI state management
4. ui/screens/ - User interface screens

Please review and provide analysis, suggestions, or answers to questions
about this codebase.
"""
    output_file.write(summary)

# ============================================================================
# MAIN EXECUTION
# ============================================================================

def main():
    """Main execution function."""
    # Determine output filename
    if len(sys.argv) > 1:
        output_filename = sys.argv[1]
    else:
        timestamp = datetime.now().strftime("%Y%m%d_%H%M%S")
        output_filename = f"morse_source_{timestamp}.txt"
    
    output_path = PROJECT_ROOT / output_filename
    
    print(f"Brute Morse Code - Source Generator")
    print(f"{'='*60}")
    print(f"Project root: {PROJECT_ROOT}")
    print(f"Output file:  {output_path}")
    print(f"{'='*60}\n")
    
    # Collect files
    print("Collecting files...")
    files = collect_files(PROJECT_ROOT)
    
    total_files = sum(len(file_list) for file_list in files.values())
    print(f"Found {total_files} files to include:")
    print(f"  - Manifest:     {len(files['manifest'])}")
    print(f"  - Gradle:       {len(files['gradle'])}")
    print(f"  - Source:       {len(files['source'])}")
    print(f"  - Resources:    {len(files['resources'])}")
    print(f"  - Docs:         {len(files['docs'])}")
    print()
    
    # Generate output
    print("Generating output file...")
    with open(output_path, 'w', encoding='utf-8') as output_file:
        # Header
        generate_header(output_file)
        
        # Write each category
        write_file_section(output_file, "Android Manifest", files['manifest'], PROJECT_ROOT)
        write_file_section(output_file, "Gradle Configuration", files['gradle'], PROJECT_ROOT)
        write_file_section(output_file, "Source Code", files['source'], PROJECT_ROOT)
        write_file_section(output_file, "Resources", files['resources'], PROJECT_ROOT)
        write_file_section(output_file, "Documentation", files['docs'], PROJECT_ROOT)
        
        # Summary
        generate_summary(output_file, files)
    
    print(f"✓ Successfully generated: {output_filename}")
    print(f"  File size: {output_path.stat().st_size:,} bytes")
    print(f"\nYou can now share this file with Claude in a new chat!")
    print(f"\nSuggested prompt:")
    print(f"  'Please review this Brute Morse Code Android app source code.'")
    print()

if __name__ == "__main__":
    try:
        main()
    except KeyboardInterrupt:
        print("\n\nOperation cancelled by user.")
        sys.exit(1)
    except Exception as e:
        print(f"\n❌ ERROR: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)