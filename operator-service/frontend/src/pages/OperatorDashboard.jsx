import { useMemo, useState } from 'react';
import { CheckCircle, PencilLine } from 'lucide-react';
import { mockRequests } from '../data/mock.js';

export default function OperatorDashboard() {
  const [requests, setRequests] = useState(mockRequests);
  const [selectedId, setSelectedId] = useState(mockRequests[0]?.id);
  const [draft, setDraft] = useState(mockRequests[0]?.aiGeneratedResponse || '');
  const [notes, setNotes] = useState('');

  const selected = useMemo(
    () => requests.find((item) => item.id === selectedId),
    [requests, selectedId]
  );

  const handleSelect = (item) => {
    setSelectedId(item.id);
    setDraft(item.aiGeneratedResponse || '');
    setNotes('');
  };

  const handleApprove = () => {
    if (!selected) return;
    setRequests((prev) => prev.filter((item) => item.id !== selected.id));
    setSelectedId(prev => (prev === selected.id ? null : prev));
  };

  const handleEditSend = () => {
    if (!selected || !draft.trim()) return;
    setRequests((prev) => prev.filter((item) => item.id !== selected.id));
    setSelectedId(prev => (prev === selected.id ? null : prev));
  };

  return (
    <div className="grid" style={{ gridTemplateColumns: '320px 1fr', gap: 20 }}>
      <div className="card" style={{ height: 'calc(100vh - 120px)', overflow: 'auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
          <h3 style={{ margin: 0 }}>Pending Requests</h3>
          <span className="tag">{requests.length} items</span>
        </div>

        <div style={{ marginTop: 16, display: 'grid', gap: 12 }}>
          {requests.map((item) => (
            <button
              key={item.id}
              onClick={() => handleSelect(item)}
              className="card"
              style={{
                textAlign: 'left',
                padding: 12,
                borderColor: item.id === selectedId ? 'rgba(40,196,161,0.6)' : undefined
              }}
            >
              <div style={{ fontSize: 13, color: 'var(--ink-soft)' }}>{item.senderEmail}</div>
              <div style={{ fontWeight: 600 }}>{item.subject}</div>
              <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                <span className="tag">{item.category}</span>
                <span className="tag">AI {(item.aiConfidenceScore * 100).toFixed(0)}%</span>
              </div>
            </button>
          ))}
        </div>
      </div>

      <div>
        {!selected ? (
          <div className="card">Select a request to review.</div>
        ) : (
          <>
            <div className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div>
                  <div className="label">From</div>
                  <div>{selected.senderEmail}</div>
                </div>
                <div>
                  <div className="label">Category</div>
                  <div>{selected.category}</div>
                </div>
              </div>
              <div style={{ marginTop: 16 }}>
                <div className="label">Subject</div>
                <div style={{ fontSize: 18, fontWeight: 600 }}>{selected.subject}</div>
              </div>
              <div style={{ marginTop: 16 }}>
                <div className="label">Message</div>
                <div style={{ color: 'var(--ink-soft)', lineHeight: 1.6 }}>{selected.emailBody}</div>
              </div>
            </div>

            <div className="card">
              <div className="label">AI draft</div>
              <div style={{ background: '#10161d', borderRadius: 12, padding: 14 }}>
                {selected.aiGeneratedResponse}
              </div>
              <div style={{ marginTop: 16, display: 'flex', gap: 12 }}>
                <button className="button" onClick={handleApprove}>
                  <CheckCircle size={16} /> Approve and send
                </button>
              </div>
            </div>

            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <PencilLine size={18} />
                <h3 style={{ margin: 0 }}>Edit response</h3>
              </div>
              <div style={{ marginTop: 12 }}>
                <label className="label">Response</label>
                <textarea
                  className="textarea"
                  rows={6}
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                />
              </div>
              <div style={{ marginTop: 12 }}>
                <label className="label">Notes (internal)</label>
                <textarea
                  className="textarea"
                  rows={3}
                  value={notes}
                  onChange={(event) => setNotes(event.target.value)}
                />
              </div>
              <button className="button" style={{ marginTop: 12 }} onClick={handleEditSend}>
                Edit and send
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
