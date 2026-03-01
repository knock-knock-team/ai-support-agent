import { useMemo, useState, useEffect } from 'react';
import { knowledgeBaseApi } from '../api/client.js';
import { Upload, FileText, Trash2, Search, AlertCircle } from 'lucide-react';

export default function KnowledgeBase() {
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('All');
  const [categories, setCategories] = useState([]);
  const [documents, setDocuments] = useState([]);
  const [searchResults, setSearchResults] = useState([]);
  const [loading, setLoading] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [error, setError] = useState(null);
  const [uploadFile, setUploadFile] = useState(null);
  const [uploadCategory, setUploadCategory] = useState('General');
  const [uploadTags, setUploadTags] = useState('');
  const [showUploadModal, setShowUploadModal] = useState(false);

  useEffect(() => {
    loadCategories();
    loadDocuments();
  }, []);

  const loadCategories = async () => {
    try {
      const cats = await knowledgeBaseApi.getCategories();
      setCategories(cats);
    } catch (err) {
      console.error('Failed to load categories:', err);
    }
  };

  const loadDocuments = async () => {
    try {
      setLoading(true);
      setError(null);
      const docs = await knowledgeBaseApi.getDocuments();
      setDocuments(docs);
    } catch (err) {
      setError('Не удалось загрузить документы');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleUpload = async () => {
    if (!uploadFile) return;

    try {
      setUploading(true);
      setError(null);
      const tags = uploadTags.split(',').map(t => t.trim()).filter(t => t);
      await knowledgeBaseApi.uploadDocument(uploadFile, uploadCategory, tags);
      setShowUploadModal(false);
      setUploadFile(null);
      setUploadTags('');
      await loadDocuments();
    } catch (err) {
      setError(err.message || 'Не удалось загрузить файл');
    } finally {
      setUploading(false);
    }
  };

  const handleSearch = async () => {
    if (!query.trim()) {
      setSearchResults([]);
      return;
    }

    try {
      setLoading(true);
      setError(null);
      const filterCategory = category !== 'All' ? category : null;
      const results = await knowledgeBaseApi.search({
        query: query,
        limit: 10,
        category: filterCategory
      });
      setSearchResults(results);
    } catch (err) {
      setError('Не удалось выполнить поиск');
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  const handleDelete = async (documentId) => {
    if (!confirm('Удалить этот документ?')) return;

    try {
      setError(null);
      await knowledgeBaseApi.deleteDocument(documentId);
      await loadDocuments();
      setSearchResults(results => results.filter(r => r.document_id !== documentId));
    } catch (err) {
      setError('Не удалось удалить документ');
      console.error(err);
    }
  };

  const filtered = useMemo(() => {
    if (searchResults.length > 0) {
      return searchResults;
    }
    return category === 'All' 
      ? documents 
      : documents.filter(doc => doc.category === category);
  }, [documents, category, searchResults]);

  const handleKeyPress = (e) => {
    if (e.key === 'Enter') {
      handleSearch();
    }
  };

  return (
    <div className="grid" style={{ gap: 16 }}>
      <div className="card">
        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap', alignItems: 'center' }}>
          <div style={{ display: 'flex', gap: 8, flex: 1, minWidth: 240 }}>
            <input
              className="input"
              style={{ flex: 1 }}
              placeholder="Семантический поиск по документам..."
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              onKeyPress={handleKeyPress}
            />
            <button 
              className="button primary" 
              onClick={handleSearch}
              disabled={loading || !query.trim()}
            >
              <Search size={16} />
              Поиск
            </button>
          </div>
          <select 
            className="select" 
            value={category} 
            onChange={(e) => {
              setCategory(e.target.value);
              setSearchResults([]);
              setQuery('');
            }}
          >
            <option value="All">Все категории</option>
            {categories.map(cat => (
              <option key={cat} value={cat}>{cat}</option>
            ))}
          </select>
          <button 
            className="button secondary" 
            onClick={() => setShowUploadModal(true)}
          >
            <Upload size={16} />
            Загрузить PDF
          </button>
        </div>

        {error && (
          <div style={{ 
            marginTop: 12, 
            padding: 12, 
            background: 'rgba(255,100,100,0.1)', 
            borderRadius: 8,
            display: 'flex',
            alignItems: 'center',
            gap: 8
          }}>
            <AlertCircle size={16} color="var(--danger)" />
            <span style={{ color: 'var(--danger)' }}>{error}</span>
          </div>
        )}
      </div>

      {loading && (
        <div className="card" style={{ textAlign: 'center', padding: 40 }}>
          <div style={{ color: 'var(--ink-soft)' }}>Загрузка...</div>
        </div>
      )}

      {!loading && filtered.length === 0 && (
        <div className="card" style={{ textAlign: 'center', padding: 40 }}>
          <FileText size={48} color="var(--ink-soft)" style={{ margin: '0 auto 16px' }} />
          <div style={{ color: 'var(--ink-soft)' }}>
            {searchResults.length === 0 && query ? 'Ничего не найдено' : 'Документы не загружены'}
          </div>
        </div>
      )}

      {!loading && filtered.length > 0 && (
        <div className="grid cols-3">
          {filtered.map((item) => (
            <div key={item.document_id || item.documentId} className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start', marginBottom: 8 }}>
                <div className="tag">{item.category}</div>
                <button
                  className="button"
                  style={{ padding: 4, minWidth: 'unset' }}
                  onClick={() => handleDelete(item.document_id || item.documentId)}
                  title="Удалить документ"
                >
                  <Trash2 size={14} />
                </button>
              </div>
              
              <h3 style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                <FileText size={18} />
                {item.filename || item.title}
              </h3>
              
              <p style={{ color: 'var(--ink-soft)', fontSize: 14, marginTop: 8 }}>
                {item.content ? item.content.substring(0, 200) + '...' : 'Нет описания'}
              </p>
              
              {item.score && (
                <div style={{ marginTop: 8, fontSize: 12, color: 'var(--accent)' }}>
                  Релевантность: {(item.score * 100).toFixed(1)}%
                </div>
              )}
              
              {item.tags && item.tags.length > 0 && (
                <div style={{ display: 'flex', gap: 6, marginTop: 10, flexWrap: 'wrap' }}>
                  {item.tags.map((tag, idx) => (
                    <span key={idx} className="tag" style={{ fontSize: 11 }}>
                      {tag}
                    </span>
                  ))}
                </div>
              )}

              {(item.page_count || item.pageCount) && (
                <div style={{ marginTop: 8, fontSize: 12, color: 'var(--ink-soft)' }}>
                  Страниц: {item.page_count || item.pageCount}
                </div>
              )}

              {(item.chunk_count || item.chunkCount) && (
                <div style={{ marginTop: 4, fontSize: 12, color: 'var(--ink-soft)' }}>
                  Фрагментов: {item.chunk_count || item.chunkCount}
                </div>
              )}
            </div>
          ))}
        </div>
      )}

      {showUploadModal && (
        <div className="modal-overlay" onClick={() => setShowUploadModal(false)}>
          <div className="modal-content" onClick={(e) => e.stopPropagation()}>
            <h2>Загрузить PDF документ</h2>
            
            <div style={{ marginTop: 20 }}>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
                Файл PDF
              </label>
              <input
                type="file"
                accept=".pdf"
                onChange={(e) => setUploadFile(e.target.files[0])}
                style={{ display: 'block', width: '100%' }}
              />
            </div>

            <div style={{ marginTop: 16 }}>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
                Категория
              </label>
              <select 
                className="select" 
                value={uploadCategory}
                onChange={(e) => setUploadCategory(e.target.value)}
                style={{ width: '100%' }}
              >
                {categories.map(cat => (
                  <option key={cat} value={cat}>{cat}</option>
                ))}
              </select>
            </div>

            <div style={{ marginTop: 16 }}>
              <label style={{ display: 'block', marginBottom: 8, fontWeight: 500 }}>
                Теги (через запятую)
              </label>
              <input
                className="input"
                value={uploadTags}
                onChange={(e) => setUploadTags(e.target.value)}
                placeholder="например: инструкция, настройка, wifi"
                style={{ width: '100%' }}
              />
            </div>

            <div style={{ display: 'flex', gap: 12, marginTop: 24 }}>
              <button
                className="button primary"
                onClick={handleUpload}
                disabled={!uploadFile || uploading}
                style={{ flex: 1 }}
              >
                {uploading ? 'Загрузка...' : 'Загрузить'}
              </button>
              <button
                className="button secondary"
                onClick={() => setShowUploadModal(false)}
                disabled={uploading}
                style={{ flex: 1 }}
              >
                Отмена
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
