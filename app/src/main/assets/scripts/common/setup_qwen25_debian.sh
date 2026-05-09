#!/bin/bash
# scripts/common/setup_qwen25_debian.sh
# Download Qwen2.5-1.5B-Instruct GGUF model for llama.cpp

handle_error() {
    echo ""
    echo "❌ FluxLinux Error: Script failed at step: $1"
    echo "---------------------------------------------------"
    echo "Please check the error message above."
    echo "---------------------------------------------------"
    rm -f "$MODEL_FILE.tmp" 2>/dev/null
    read -p "Press Enter to exit..."
    exit 1
}

echo "FluxLinux: Setting up Qwen2.5-1.5B-Instruct Model..."

# --- 1. Check llama.cpp is installed ---
echo "FluxLinux: Checking llama.cpp installation..."
if [ ! -f "/usr/local/bin/llama-vulkan" ] && [ ! -f "/usr/local/bin/llama-cli" ]; then
    echo " [❌] llama.cpp not found! Install 'Vulkan Llama.cpp' first."
    handle_error "llama.cpp not installed"
fi
echo " [✅] llama.cpp found."

# --- 2. Create models directory ---
MODELS_DIR="/root/models"
mkdir -p "$MODELS_DIR" || handle_error "Create models directory"
echo " [✅] Models directory: $MODELS_DIR"

# --- 3. Download model ---
MODEL_URL="https://huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF/resolve/main/Qwen2.5-1.5B-Instruct-Q4_0.gguf"
MODEL_FILE="$MODELS_DIR/Qwen2.5-1.5B-Instruct-Q4_0.gguf"
EXPECTED_SIZE=983451136  # ~938 MB in bytes

if [ -f "$MODEL_FILE" ]; then
    FILE_SIZE=$(stat -c%s "$MODEL_FILE" 2>/dev/null || echo "0")
    if [ "$FILE_SIZE" -eq "$EXPECTED_SIZE" ]; then
        echo " [✅] Model already downloaded and verified: $MODEL_FILE"
    else
        echo " [⚠️] Model file size mismatch ($FILE_SIZE vs $EXPECTED_SIZE bytes)."
        echo "      Re-downloading..."
        rm -f "$MODEL_FILE"
    fi
fi

if [ ! -f "$MODEL_FILE" ]; then
    echo "FluxLinux: Downloading Qwen2.5-1.5B-Instruct Q4_0 (938 MB)..."
    echo "Source: huggingface.co/bartowski/Qwen2.5-1.5B-Instruct-GGUF"
    echo ""
    curl -L -C - -o "$MODEL_FILE" \
        --progress-bar \
        --fail \
        --retry 3 \
        --connect-timeout 30 \
        --max-time 600 \
        "$MODEL_URL" \
        || handle_error "Download Model"

    # Verify file size
    DL_SIZE=$(stat -c%s "$MODEL_FILE" 2>/dev/null || echo "0")
    if [ "$DL_SIZE" -ne "$EXPECTED_SIZE" ]; then
        echo " [❌] Download size mismatch: got $DL_SIZE bytes, expected $EXPECTED_SIZE"
        echo "      The download may have been interrupted. Delete the file and retry."
        rm -f "$MODEL_FILE"
        handle_error "Download incomplete"
    fi

    # Verify GGUF magic bytes
    MAGIC=$(xxd -l 4 -p "$MODEL_FILE" 2>/dev/null || echo "")
    if [ "$MAGIC" != "47475546" ]; then
        echo " [❌] File is not a valid GGUF model (magic bytes mismatch)"
        echo "      Delete the file and retry the download."
        rm -f "$MODEL_FILE"
        handle_error "Invalid GGUF file"
    fi

    echo ""
    echo " [✅] Model downloaded and verified!"
    echo "      Size: $(du -h "$MODEL_FILE" | cut -f1)"
fi

# --- 4. Verify installation ---
echo " [✅] Model verified: $MODEL_FILE"

echo ""
echo "============================================"
echo "  ✅ Qwen2.5-1.5B-Instruct Model Ready!"
echo "============================================"
echo ""
echo "Model: $MODEL_FILE"
echo "Size:  $(du -h "$MODEL_FILE" | cut -f1)"
echo "Quant: Q4_0 (Standard, Vulkan GPU compatible)"
echo ""
echo "Run: llama-vulkan llama-cli -m $MODEL_FILE -p 'Hello' -ngl 99"
echo "============================================"
