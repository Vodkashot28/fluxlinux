#!/bin/bash
# scripts/common/setup_qwen35_debian.sh
# Download Qwen3.5-0.8B GGUF model for llama.cpp
# Model: https://huggingface.co/unsloth/Qwen3.5-0.8B-GGUF

# Error Handler Function to pause and let user read logs
handle_error() {
    echo ""
    echo "❌ FluxLinux Error: Script failed at step: $1"
    echo "---------------------------------------------------"
    echo "Please check the error message above."
    echo "You can copy the error output to share with support."
    echo "---------------------------------------------------"
    rm -f "$MODEL_FILE.tmp" 2>/dev/null
    read -p "Press Enter to exit..."
    exit 1
}

echo "FluxLinux: Setting up Qwen3.5-0.8B Model..."

# --- 1. Check llama.cpp is installed ---
echo "FluxLinux: Checking llama.cpp installation..."
if [ ! -f "/usr/local/bin/llama-vulkan" ] && [ ! -f "/usr/local/bin/llama-cli" ]; then
    echo " [❌] llama.cpp not found!"
    echo "      Install 'Vulkan Llama.cpp' first from distro settings."
    handle_error "llama.cpp not installed"
fi
echo " [✅] llama.cpp found."

# --- 2. Create models directory ---
MODELS_DIR="/root/models"
mkdir -p "$MODELS_DIR" || handle_error "Create models directory"
echo " [✅] Models directory: $MODELS_DIR"

# --- 3. Download model ---
MODEL_URL="https://huggingface.co/unsloth/Qwen3.5-0.8B-GGUF/resolve/main/Qwen3.5-0.8B-Q4_0.gguf"
MODEL_FILE="$MODELS_DIR/Qwen3.5-0.8B-Q4_0.gguf"

if [ -f "$MODEL_FILE" ]; then
    FILE_SIZE=$(stat -c%s "$MODEL_FILE" 2>/dev/null || echo "0")
    if [ "$FILE_SIZE" -gt 1000000 ]; then
        echo " [✅] Model already downloaded: $MODEL_FILE"
        echo "      Size: $(du -h "$MODEL_FILE" | cut -f1)"
    else
        echo " [⚠️] Model file exists but appears incomplete ($FILE_SIZE bytes). Re-downloading..."
        rm -f "$MODEL_FILE"
    fi
fi

if [ ! -f "$MODEL_FILE" ]; then
    echo "FluxLinux: Downloading Qwen3.5-0.8B Q4_0 (507 MB)..."
    echo "Source: huggingface.co/unsloth/Qwen3.5-0.8B-GGUF"
    echo ""
    curl -L -o "$MODEL_FILE.tmp" \
        --progress-bar \
        --fail \
        --retry 3 \
        "$MODEL_URL" \
        || handle_error "Download Model"

    # Verify download
    if [ ! -f "$MODEL_FILE.tmp" ]; then
        handle_error "Download Model (temp file missing)"
    fi

    DL_SIZE=$(stat -c%s "$MODEL_FILE.tmp" 2>/dev/null || echo "0")
    if [ "$DL_SIZE" -lt 1000000 ]; then
        echo " [❌] Download failed: file too small ($DL_SIZE bytes)"
        handle_error "Download Model (incomplete)"
    fi

    mv "$MODEL_FILE.tmp" "$MODEL_FILE" || handle_error "Move model file"
    echo ""
    echo " [✅] Model downloaded successfully!"
    echo "      Size: $(du -h "$MODEL_FILE" | cut -f1)"
fi

# --- 4. Verify installation ---
if [ ! -f "$MODEL_FILE" ]; then
    handle_error "Model file not found after download"
fi
echo " [✅] Model verified: $MODEL_FILE"

echo ""
echo "============================================"
echo "  ✅ Qwen3.5-0.8B Model Ready!"
echo "============================================"
echo ""
echo "Model: $MODEL_FILE"
echo "Size:  $(du -h "$MODEL_FILE" | cut -f1)"
echo "Quant: Q4_0 (best for Adreno GPU)"
echo ""
echo "Run with GPU:"
echo "  llama-vulkan llama-cli -m $MODEL_FILE -p 'Hello' -ngl 99"
echo ""
echo "Interactive chat:"
echo "  llama-vulkan llama-cli -m $MODEL_FILE -cnv -ngl 99"
echo ""
echo "Start server:"
echo "  llama-vulkan llama-server -m $MODEL_FILE --port 8080 -ngl 99"
echo ""
echo "Use -ngl 99 to offload all layers to GPU."
echo "============================================"
