import { useMemo, useState } from 'react';
import { mockKnowledge } from '../data/mock.js';

export default function KnowledgeBase() {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('All');

  const filtered = useMemo(() => {
    return mockKnowledge.filter((item) => {
      const matchesQuery =
        item.title.toLowerCase().includes(query.toLowerCase()) ||
        item.content.toLowerCase().includes(query.toLowerCase());
      const matchesCategory = category === 'All' || item.category === category;
      return matchesQuery && matchesCategory;
    });
  }, [query, category]);

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="card">
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <input
            className="input"
            style={{ flex: 1, minWidth: 240 }}
            placeholder="Search knowledge base"
            value={query}
            onChange={(event) => setQuery(event.target.value)}
          />
          <select className="select" value={category} onChange={(event) => setCategory(event.target.value)}>
            <option>All</option>
            <option>Technical</option>
            <option>Billing</option>
            <option>General</option>
          </select>
          <button className="button secondary">Add article</button>
        </div>
      </div>

      <div className="grid cols-3">
        {filtered.map((item) => (
          <div key={item.id} className="card">
            <div className="tag">{item.category}</div>
            <h3>{item.title}</h3>
            <p style={{ color: 'var(--ink-soft)' }}>{item.content}</p>
            <div style={{ display: 'flex', gap: 8, marginTop: 10 }}>
              {item.tags.map((tag) => (
                <span key={tag} className="tag">
                  {tag}
                </span>
              ))}
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}
