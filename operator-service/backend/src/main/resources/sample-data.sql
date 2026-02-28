-- Sample data for testing
-- Run this after the initial schema.sql

-- Insert sample operators (password: Operator123!)
INSERT INTO users (username, password, full_name, role, first_login, active, created_at, updated_at)
VALUES 
    ('operator1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Alice Johnson', 'ROLE_OPERATOR', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('operator2', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Bob Smith', 'ROLE_OPERATOR', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('operator3', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Carol Williams', 'ROLE_OPERATOR', TRUE, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (username) DO NOTHING;

-- Insert sample requests with low confidence (requiring operator review)
INSERT INTO requests (subject, user_message, ai_response, confidence, status, category, sender_email, created_at, updated_at)
VALUES 
    (
        'Cannot access my account',
        'I forgot my password and the reset email is not arriving. What should I do?',
        'Please check your spam folder for the password reset email. If you still don''t receive it, contact our support team.',
        0.62,
        'PENDING',
        'ACCOUNT',
        'user1@example.com',
        CURRENT_TIMESTAMP - INTERVAL '2 hours',
        CURRENT_TIMESTAMP - INTERVAL '2 hours'
    ),
    (
        'Billing issue with last invoice',
        'I was charged twice for my subscription this month. Can you please refund the duplicate charge?',
        'I understand you were charged twice. Our billing team will investigate and process a refund within 3-5 business days.',
        0.58,
        'PENDING',
        'BILLING',
        'user2@example.com',
        CURRENT_TIMESTAMP - INTERVAL '1 hour',
        CURRENT_TIMESTAMP - INTERVAL '1 hour'
    ),
    (
        'Technical problem with API',
        'When I try to connect to your API endpoint, I get a 500 error. Is there an ongoing issue?',
        'We are experiencing some technical difficulties with our API. Our team is working to resolve this.',
        0.45,
        'PENDING',
        'TECHNICAL',
        'developer@company.com',
        CURRENT_TIMESTAMP - INTERVAL '30 minutes',
        CURRENT_TIMESTAMP - INTERVAL '30 minutes'
    ),
    (
        'How to upgrade my plan',
        'I want to upgrade from Basic to Premium plan. What are the steps?',
        'To upgrade your plan, go to Settings > Subscription > Change Plan and select Premium. The change will be effective immediately.',
        0.85,
        'APPROVED',
        'GENERAL',
        'user3@example.com',
        CURRENT_TIMESTAMP - INTERVAL '3 hours',
        CURRENT_TIMESTAMP - INTERVAL '3 hours'
    ),
    (
        'Feature request',
        'It would be great if you could add dark mode to the dashboard.',
        'Thank you for your feedback! We''ve noted your feature request and will consider it for future updates.',
        0.78,
        'APPROVED',
        'GENERAL',
        'user4@example.com',
        CURRENT_TIMESTAMP - INTERVAL '5 hours',
        CURRENT_TIMESTAMP - INTERVAL '5 hours'
    ),
    (
        'Cannot upload files',
        'Every time I try to upload a file larger than 10MB, I get an error. Is this a limitation?',
        'The file size limit is 25MB. If you''re getting errors with smaller files, please try a different browser or clear your cache.',
        0.55,
        'PENDING',
        'TECHNICAL',
        'user5@example.com',
        CURRENT_TIMESTAMP - INTERVAL '45 minutes',
        CURRENT_TIMESTAMP - INTERVAL '45 minutes'
    ),
    (
        'Question about data privacy',
        'How is my personal data stored and protected on your platform?',
        'We use industry-standard encryption and follow GDPR guidelines. Your data is stored securely and never shared with third parties.',
        0.68,
        'PENDING',
        'GENERAL',
        'privacy@example.com',
        CURRENT_TIMESTAMP - INTERVAL '20 minutes',
        CURRENT_TIMESTAMP - INTERVAL '20 minutes'
    ),
    (
        'Integration with third-party tools',
        'Do you offer integration with Slack for notifications?',
        'Yes, we offer Slack integration. You can set it up in Settings > Integrations > Slack.',
        0.92,
        'APPROVED',
        'TECHNICAL',
        'admin@company.com',
        CURRENT_TIMESTAMP - INTERVAL '6 hours',
        CURRENT_TIMESTAMP - INTERVAL '6 hours'
    );

-- Set some requests as reviewed by operators
UPDATE requests SET 
    status = 'IN_REVIEW',
    operator_id = (SELECT id FROM users WHERE username = 'operator1' LIMIT 1),
    operator_notes = 'Need to verify billing details before responding'
WHERE subject = 'Billing issue with last invoice';

UPDATE requests SET 
    status = 'SENT',
    operator_id = (SELECT id FROM users WHERE username = 'operator2' LIMIT 1),
    operator_response = 'Hello! Thank you for reaching out. To upgrade your plan, please log in to your account, navigate to Settings > Billing > Change Plan, and select the Premium option. The upgrade will be applied immediately, and you''ll be charged a prorated amount for the current billing period. If you need any assistance, feel free to contact us. Best regards, Support Team',
    responded_at = CURRENT_TIMESTAMP - INTERVAL '2 hours'
WHERE subject = 'How to upgrade my plan';
