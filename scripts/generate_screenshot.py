import sys
import os
from PIL import Image, ImageDraw, ImageFont

# --- Configuration ---
INPUT_FILE = "output/live_run.txt"
OUTPUT_FILE_PREFIX = "output/screenshot"
BG_COLOR = "#2E3440"  # Nord-like dark background
TEXT_COLOR = "#E5E9F0"  # Nord-like light text
PROMPT_COLOR = "#A3BE8C"  # A green-ish color for commands
FONT_SIZE = 15
LINE_SPACING = 5
PADDING = 20
MAX_IMAGE_HEIGHT = 800  # Max height in pixels for one image part
PROMPT = "$ "

# --- Font Selection ---
# Try to find a common monospaced font.
# On Debian/Ubuntu, 'DejaVuSansMono.ttf' is a good default.
try:
    font = ImageFont.truetype("DejaVuSansMono.ttf", FONT_SIZE)
except IOError:
    print("Default monospaced font not found. Using Pillow's default.", file=sys.stderr)
    try:
        font = ImageFont.load_default(size=FONT_SIZE)
    except AttributeError: # For older Pillow versions
        font = ImageFont.load_default()

def get_text_size(draw, text, font):
    """Get bounding box of text using the modern textbbox method."""
    if hasattr(draw, "textbbox"):
        bbox = draw.textbbox((0, 0), text, font=font)
        return bbox[2] - bbox[0], bbox[3] - bbox[1]
    else: # Fallback for older Pillow versions
        return draw.textsize(text, font=font)

def main():
    """
    Generates terminal-style screenshots from a long text file,
    splitting it into multiple parts if necessary.
    """
    # Ensure the output directory exists
    output_dir = os.path.dirname(OUTPUT_FILE_PREFIX)
    if output_dir and not os.path.exists(output_dir):
        os.makedirs(output_dir)

    try:
        with open(INPUT_FILE, "r") as f:
            lines = f.read().splitlines()
    except FileNotFoundError:
        print(f"Error: Input file '{INPUT_FILE}' not found.", file=sys.stderr)
        sys.exit(1)

    # Prepend prompts to the command lines for styling
    styled_lines = []
    for i, line in enumerate(lines):
        if i < 3:  # First 3 lines are assumed to be commands
            styled_lines.append(PROMPT + line)
        else:
            styled_lines.append(line)

    # Prepare to draw to calculate dimensions for all lines
    temp_img = Image.new("RGB", (1, 1))
    draw = ImageDraw.Draw(temp_img)

    max_width = 0
    line_heights = []
    for line in styled_lines:
        width, height = get_text_size(draw, line, font)
        max_width = max(max_width, width)
        line_heights.append(height)

    img_width = max_width + PADDING * 2
    
    # --- Pagination Logic ---
    part_number = 1
    current_line_index = 0
    
    while current_line_index < len(styled_lines):
        lines_for_this_part = []
        part_height_calculator = PADDING  # Start with top padding
        
        part_start_index = current_line_index
        while current_line_index < len(styled_lines):
            line_h = line_heights[current_line_index] + LINE_SPACING
            # Check if adding the next line exceeds max height.
            # Also ensure at least one line is added to avoid infinite loops.
            if part_height_calculator + line_h > MAX_IMAGE_HEIGHT and lines_for_this_part:
                break
            
            part_height_calculator += line_h
            lines_for_this_part.append(styled_lines[current_line_index])
            current_line_index += 1
        
        part_height_calculator += PADDING  # Add bottom padding

        # Create the image for the current part
        img = Image.new("RGB", (int(img_width), int(part_height_calculator)), BG_COLOR)
        draw = ImageDraw.Draw(img)

        # Draw the text for this part
        y_text = PADDING
        for i, line in enumerate(lines_for_this_part):
            original_line_index = part_start_index + i
            
            if line.startswith(PROMPT):
                prompt_width, _ = get_text_size(draw, PROMPT, font)
                draw.text((PADDING, y_text), PROMPT, font=font, fill=PROMPT_COLOR)
                draw.text((PADDING + prompt_width, y_text), line[len(PROMPT):], font=font, fill=TEXT_COLOR)
            else:
                draw.text((PADDING, y_text), line, font=font, fill=TEXT_COLOR)
            
            y_text += line_heights[original_line_index] + LINE_SPACING
            
        # Save the image part
        output_filename = f"{OUTPUT_FILE_PREFIX}_{part_number}.png"
        img.save(output_filename)
        print(f"Screenshot part saved to '{output_filename}'")
        
        part_number += 1

if __name__ == "__main__":
    main()