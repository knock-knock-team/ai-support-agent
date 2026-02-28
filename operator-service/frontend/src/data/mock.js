export const mockRequests = [
  {
    id: 1023,
    senderEmail: 'aleks@client.io',
    subject: 'Billing refund request',
    emailBody: 'I was charged twice for last month. Please advise.',
    category: 'BILLING',
    status: 'PENDING',
    aiConfidenceScore: 0.62,
    aiGeneratedResponse:
      'We are sorry for the duplicate charge. We have initiated a refund and it will be processed within 5-7 business days.'
  },
  {
    id: 1024,
    senderEmail: 'irina@startup.app',
    subject: 'Login issues on mobile',
    emailBody: 'Users are reporting 2FA errors when logging in from Android.',
    category: 'TECHNICAL',
    status: 'PENDING',
    aiConfidenceScore: 0.54,
    aiGeneratedResponse:
      'Thanks for reporting the issue. Please ask affected users to reinstall the app and retry 2FA.'
  },
  {
    id: 1025,
    senderEmail: 'support@market.ru',
    subject: 'Invoice missing',
    emailBody: 'Invoice for January is missing from billing section.',
    category: 'BILLING',
    status: 'PENDING',
    aiConfidenceScore: 0.44,
    aiGeneratedResponse:
      'We can generate and resend the invoice. Could you confirm the billing email address?'
  }
];

export const mockStats = {
  total: 128,
  pending: 18,
  approved: 72,
  edited: 38,
  byCategory: [
    { name: 'Technical', value: 54 },
    { name: 'Billing', value: 46 },
    { name: 'General', value: 28 }
  ],
  byStatus: [
    { name: 'Pending', value: 18 },
    { name: 'Approved', value: 72 },
    { name: 'Edited', value: 38 }
  ]
};

export const mockUsers = [
  {
    id: 1,
    username: 'admin',
    name: 'Admin User',
    role: 'ROLE_ADMIN',
    status: 'Active'
  },
  {
    id: 2,
    username: 'operator01',
    name: 'Operator One',
    role: 'ROLE_OPERATOR',
    status: 'Active'
  },
  {
    id: 3,
    username: 'operator02',
    name: 'Operator Two',
    role: 'ROLE_OPERATOR',
    status: 'Inactive'
  }
];

export const mockKnowledge = [
  {
    id: 'kb-001',
    title: 'Password reset workflow',
    category: 'Technical',
    tags: ['password', 'auth'],
    content: 'Guide customers to the reset form and verify MFA is enabled.'
  },
  {
    id: 'kb-002',
    title: 'Refunds and chargebacks',
    category: 'Billing',
    tags: ['refund', 'billing'],
    content: 'Refunds are processed within 5-7 business days. Confirm billing email.'
  },
  {
    id: 'kb-003',
    title: 'Service uptime policy',
    category: 'General',
    tags: ['sla'],
    content: 'Uptime commitments apply to paid plans. Provide the status page link.'
  }
];
