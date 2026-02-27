import { useEffect, useMemo, useState } from 'react';
import { Archive, Filter, RefreshCw, Search, ChevronLeft, ChevronRight } from 'lucide-react';
import { operatorApi } from '../api/client.js';

const CATEGORY_OPTIONS = [
  { value: 'ALL', label: 'Все категории' },
  { value: 'TECHNICAL', label: 'Технические' },
  { value: 'BILLING', label: 'Финансы' },
  { value: 'ACCOUNT', label: 'Аккаунт' },
  { value: 'GENERAL', label: 'Общие' },
  { value: 'OTHER', label: 'Другое' }
];

const ITEMS_PER_PAGE = 25;

const formatDate = (value) => {
  if (!value) return '—';
  return new Date(value).toLocaleString('ru-RU');
};

export default function RequestsArchive() {
  const [requests, setRequests] = useState([]);
  const [loading, setLoading] = useState(false);
  const [query, setQuery] = useState('');
  const [category, setCategory] = useState('ALL');
  const [project, setProject] = useState('');
  const [dateFrom, setDateFrom] = useState('');
  const [dateTo, setDateTo] = useState('');
  const [currentPage, setCurrentPage] = useState(1);

  const loadArchive = async () => {
    try {
      setLoading(true);
      const data = await operatorApi.getClosedRequests();
      setRequests(data);
    } catch (error) {
      alert(error.response?.data?.message || 'Не удалось загрузить архив заявок');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadArchive();
  }, []);

  const projectOptions = useMemo(() => {
    const values = Array.from(new Set(requests.map((item) => item.project).filter(Boolean)));
    return values.sort((a, b) => a.localeCompare(b, 'ru'));
  }, [requests]);

  const filteredRequests = useMemo(() => {
    const normalizedQuery = query.trim().toLowerCase();

    const filtered = requests.filter((item) => {
      if (category !== 'ALL' && item.category !== category) return false;
      if (project && item.project !== project) return false;

      if (dateFrom) {
        const from = new Date(`${dateFrom}T00:00:00`);
        if (new Date(item.created_at) < from) return false;
      }

      if (dateTo) {
        const to = new Date(`${dateTo}T23:59:59`);
        if (new Date(item.created_at) > to) return false;
      }

      if (!normalizedQuery) return true;

      const haystack = [
        item.email,
        item.fio,
        item.organization,
        item.serial_number,
        item.device_type,
        item.project,
        item.operator_answer,
        item.ai_generated_answer
      ]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();

      return haystack.includes(normalizedQuery);
    });

    // Reset to page 1 when filters change
    setCurrentPage(1);
    return filtered;
  }, [requests, category, project, dateFrom, dateTo, query]);

  const total = requests.length;
  const filtered = filteredRequests.length;
  const totalPages = Math.ceil(filtered / ITEMS_PER_PAGE);
  
  const paginatedRequests = useMemo(() => {
    const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
    const endIndex = startIndex + ITEMS_PER_PAGE;
    return filteredRequests.slice(startIndex, endIndex);
  }, [filteredRequests, currentPage]);

  const handlePageChange = (page) => {
    setCurrentPage(page);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="grid" style={{ gap: 20 }}>
      <div className="card">
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16 }}>
          <div>
            <h2 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 10 }}>
              <Archive size={20} /> Архив закрытых заявок
            </h2>
            <div style={{ marginTop: 8, color: 'var(--ink-soft)' }}>
              Всего: <span className="tag">{total}</span> · По фильтру: <span className="tag">{filtered}</span>
              {totalPages > 0 && <> · Страница: <span className="tag">{currentPage} из {totalPages}</span></>}
            </div>
          </div>
          <button className="button secondary" onClick={loadArchive}>
            <RefreshCw size={16} /> Обновить
          </button>
        </div>
      </div>

      <div className="card">
        <div style={{ display: 'flex', alignItems: 'center', gap: 10, marginBottom: 12 }}>
          <Filter size={16} />
          <strong>Фильтры</strong>
        </div>

        <div className="grid cols-4">
          <div>
            <label className="label">Поиск</label>
            <div style={{ position: 'relative' }}>
              <Search size={16} style={{ position: 'absolute', left: 10, top: 11, color: 'var(--ink-soft)' }} />
              <input
                className="input"
                style={{ paddingLeft: 34 }}
                placeholder="Email, ФИО, проект, ответ..."
                value={query}
                onChange={(event) => setQuery(event.target.value)}
              />
            </div>
          </div>

          <div>
            <label className="label">Категория</label>
            <select className="select" value={category} onChange={(event) => setCategory(event.target.value)}>
              {CATEGORY_OPTIONS.map((option) => (
                <option key={option.value} value={option.value}>
                  {option.label}
                </option>
              ))}
            </select>
          </div>

          <div>
            <label className="label">Проект</label>
            <select className="select" value={project} onChange={(event) => setProject(event.target.value)}>
              <option value="">Все проекты</option>
              {projectOptions.map((value) => (
                <option key={value} value={value}>
                  {value}
                </option>
              ))}
            </select>
          </div>

          <div style={{ display: 'flex', alignItems: 'end', gap: 8 }}>
            <button
              className="button secondary"
              style={{ width: '100%' }}
              onClick={() => {
                setQuery('');
                setCategory('ALL');
                setProject('');
                setDateFrom('');
                setDateTo('');
              }}
            >
              Сбросить фильтры
            </button>
          </div>
        </div>

        <div className="grid cols-4" style={{ marginTop: 12 }}>
          <div>
            <label className="label">Дата от</label>
            <input className="input" type="date" value={dateFrom} onChange={(event) => setDateFrom(event.target.value)} />
          </div>
          <div>
            <label className="label">Дата до</label>
            <input className="input" type="date" value={dateTo} onChange={(event) => setDateTo(event.target.value)} />
          </div>
        </div>
      </div>

      <div className="card" style={{ overflowX: 'auto' }}>
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Email</th>
              <th>Клиент</th>
              <th>Организация</th>
              <th>Категория</th>
              <th>Проект</th>
              <th>Уверенность AI</th>
              <th>Создано</th>
              <th>Отправлено</th>
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={9} style={{ color: 'var(--ink-soft)' }}>
                  Загрузка...
                </td>
              </tr>
            ) : filteredRequests.length === 0 ? (
              <tr>
                <td colSpan={9} style={{ color: 'var(--ink-soft)' }}>
                  По текущим фильтрам ничего не найдено
                </td>
              </tr>
            ) : (
              paginatedRequests.map((item) => (
                <tr key={item.id}>
                  <td>{item.id}</td>
                  <td>{item.email || '—'}</td>
                  <td>{item.fio || '—'}</td>
                  <td>{item.organization || '—'}</td>
                  <td><span className="tag">{item.category || '—'}</span></td>
                  <td>{item.project || '—'}</td>
                  <td>{((item.confidence_score || 0) * 100).toFixed(0)}%</td>
                  <td>{formatDate(item.created_at)}</td>
                  <td>{formatDate(item.responded_at)}</td>
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {totalPages > 1 && (
        <div className="card" style={{ display: 'flex', justifyContent: 'center', alignItems: 'center', gap: 12 }}>
          <button
            className="button secondary"
            onClick={() => handlePageChange(currentPage - 1)}
            disabled={currentPage === 1}
            style={{ opacity: currentPage === 1 ? 0.4 : 1, cursor: currentPage === 1 ? 'not-allowed' : 'pointer' }}
          >
            <ChevronLeft size={16} /> Предыдущая
          </button>

          <div style={{ display: 'flex', gap: 8, alignItems: 'center' }}>
            {Array.from({ length: totalPages }, (_, i) => i + 1).map((page) => {
              // Show first page, last page, current page and 2 pages around current
              const showPage =
                page === 1 ||
                page === totalPages ||
                (page >= currentPage - 2 && page <= currentPage + 2);

              const showEllipsisBefore = page === currentPage - 3 && currentPage > 4;
              const showEllipsisAfter = page === currentPage + 3 && currentPage < totalPages - 3;

              if (!showPage && !showEllipsisBefore && !showEllipsisAfter) return null;

              if (showEllipsisBefore || showEllipsisAfter) {
                return (
                  <span key={page} style={{ color: 'var(--ink-soft)', padding: '0 4px' }}>
                    ...
                  </span>
                );
              }

              return (
                <button
                  key={page}
                  className={page === currentPage ? 'button' : 'button secondary'}
                  onClick={() => handlePageChange(page)}
                  style={{
                    minWidth: 40,
                    padding: '8px 12px',
                    fontFamily: 'var(--mono)',
                    fontSize: 13
                  }}
                >
                  {page}
                </button>
              );
            })}
          </div>

          <button
            className="button secondary"
            onClick={() => handlePageChange(currentPage + 1)}
            disabled={currentPage === totalPages}
            style={{ opacity: currentPage === totalPages ? 0.4 : 1, cursor: currentPage === totalPages ? 'not-allowed' : 'pointer' }}
          >
            Следующая <ChevronRight size={16} />
          </button>
        </div>
      )}
    </div>
  );
}
