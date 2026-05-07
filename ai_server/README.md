# AI Flashcard Server

## Cài đặt

```bash
cd ai_server
pip install -r requirements.txt
```

## Đặt model

Copy model vào thư mục `ai_server/`:
- `gemma3-1b-it-int4.task` (hoặc bất kỳ file .gguf/.bin)

## Chạy server

```bash
python server.py
```

Hoặc chỉ định model:
```bash
python server.py --model path/to/model.gguf
```

## Test

- API docs: http://localhost:8000/docs
- Status: http://localhost:8000/status
