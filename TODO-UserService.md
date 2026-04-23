# UserService Correction TODO

## Plan Execution

### 1. Config ✅
Added to application.properties:
- `app.resume.max-size=10485760` (10MB)
- `app.resume.max-text-length=100000`

### 2. UserService.java ✅ Planned fixes
- Strict MIME `application/pdf`
- Filename `.pdf` check
- Config max size/text length
- Trim/limit extracted text

### 3. Test
- Upload PDF → check resumeText saved (dashboard)
- Invalid file → proper error

✅ Complete

