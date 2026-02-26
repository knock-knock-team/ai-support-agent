-- ============================================
-- SQL Scripts for Mail Processor Database
-- ============================================

-- View all requests
SELECT 
    id,
    email,
    organization,
    fio,
    phone,
    device_type,
    category,
    status,
    created_at
FROM requests
ORDER BY created_at DESC;

-- Count requests by status
SELECT 
    status,
    COUNT(*) as count
FROM requests
GROUP BY status;

-- Count requests by category
SELECT 
    category,
    COUNT(*) as count
FROM requests
GROUP BY category
ORDER BY count DESC;

-- Find recent requests (last 24 hours)
SELECT 
    id,
    email,
    fio,
    device_type,
    category,
    status,
    created_at
FROM requests
WHERE created_at > NOW() - INTERVAL '24 hours'
ORDER BY created_at DESC;

-- Find requests with attachments
SELECT 
    id,
    email,
    file_name,
    file_content_type,
    created_at
FROM requests
WHERE file IS NOT NULL;

-- Search by email
SELECT * 
FROM requests 
WHERE email LIKE '%example.com%';

-- Search by organization
SELECT * 
FROM requests 
WHERE organization ILIKE '%компания%';

-- Update request status
UPDATE requests
SET 
    status = 'AI_GENERATED',
    ai_generated_answer = 'Your AI generated answer here',
    updated_at = NOW()
WHERE id = 'your-uuid-here';

-- Get requests needing AI processing
SELECT 
    id,
    email,
    email_subject,
    email_body,
    device_type,
    category
FROM requests
WHERE status = 'NEW'
ORDER BY created_at ASC
LIMIT 10;

-- Statistics by date
SELECT 
    DATE(created_at) as date,
    COUNT(*) as total_requests,
    COUNT(CASE WHEN status = 'CLOSED' THEN 1 END) as closed_requests
FROM requests
GROUP BY DATE(created_at)
ORDER BY date DESC;

-- Find requests without required fields
SELECT 
    id,
    email,
    CASE 
        WHEN organization IS NULL THEN 'Missing organization'
        WHEN fio IS NULL THEN 'Missing FIO'
        WHEN phone IS NULL THEN 'Missing phone'
        WHEN device_type IS NULL THEN 'Missing device_type'
        ELSE 'Complete'
    END as missing_field
FROM requests
WHERE organization IS NULL 
   OR fio IS NULL 
   OR phone IS NULL 
   OR device_type IS NULL;

-- Export to JSON (PostgreSQL 9.3+)
SELECT json_agg(row_to_json(requests))
FROM (
    SELECT 
        id,
        email,
        organization,
        fio,
        phone,
        device_type,
        category,
        status
    FROM requests
    WHERE status = 'NEW'
) requests;

-- Cleanup old closed requests (older than 90 days)
DELETE FROM requests
WHERE status = 'CLOSED'
  AND created_at < NOW() - INTERVAL '90 days';

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_requests_status ON requests(status);
CREATE INDEX IF NOT EXISTS idx_requests_email ON requests(email);
CREATE INDEX IF NOT EXISTS idx_requests_created_at ON requests(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_requests_category ON requests(category);
