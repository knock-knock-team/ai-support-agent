import { useEffect, useMemo, useState } from 'react';
import { Archive, Filter, RefreshCw, Search, ChevronLeft, ChevronRight, X, Calendar, Download, FileText, FileSpreadsheet, Mail } from 'lucide-react';
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
  const [tempDateFrom, setTempDateFrom] = useState('');
  const [tempDateTo, setTempDateTo] = useState('');
  const [currentPage, setCurrentPage] = useState(1);
  const [selectedRequest, setSelectedRequest] = useState(null);

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

  const handleApplyDateFilter = () => {
    setDateFrom(tempDateFrom);
    setDateTo(tempDateTo);
  };

  const handleRowClick = (request) => {
    setSelectedRequest(request);
  };

  const exportToCSV = () => {
    if (filteredRequests.length === 0) {
      alert('Нет данных для экспорта');
      return;
    }

    const headers = [
      'ID', 'Источник', 'Email', 'ФИО', 'Организация', 'Телефон', 'Категория', 'Проект',
      'Тип устройства', 'Серийный номер', 'ИНН', 'Страна/Регион',
      'Уверенность AI', 'Исходный вопрос', 'Ответ AI', 'Ответ оператора', 'Заметки',
      'Статус', 'Оператор', 'Создано', 'Обновлено', 'Отправлено'
    ];

    const rows = filteredRequests.map((item) => [
      item.id,
      item.is_form ? 'Форма' : 'Email',
      item.email || '',
      item.fio || '',
      item.organization || '',
      item.phone || '',
      item.category || '',
      item.project || '',
      item.device_type || '',
      item.serial_number || '',
      item.inn || '',
      item.country_region || '',
      (item.confidence_score || 0) * 100,
      item.user_message || '',
      item.ai_generated_answer || '',
      item.operator_answer || '',
      item.operator_notes || '',
      item.status || '',
      item.operator?.username || item.operator?.name || '',
      formatDate(item.created_at),
      formatDate(item.updated_at),
      formatDate(item.responded_at)
    ]);

    const csv = [
      headers.map(h => `"${h}"`).join(','),
      ...rows.map(row => row.map(cell => {
        const str = String(cell || '').replace(/"/g, '""');
        return `"${str}"`;
      }).join(','))
    ].join('\n');

    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `archive-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
  };

  const exportToExcel = () => {
    if (filteredRequests.length === 0) {
      alert('Нет данных для экспорта');
      return;
    }

    const headers = [
      'ID', 'Источник', 'Email', 'ФИО', 'Организация', 'Телефон', 'Категория', 'Проект',
      'Тип устройства', 'Серийный номер', 'ИНН', 'Страна/Регион',
      'Уверенность AI', 'Исходный вопрос', 'Ответ AI', 'Ответ оператора', 'Заметки',
      'Статус', 'Оператор', 'Создано', 'Обновлено', 'Отправлено'
    ];

    let html = '<table border="1"><thead><tr>';
    headers.forEach(h => {
      html += `<th style="background:#28c4a1;color:white;padding:10px;text-align:left;font-weight:bold;">${h}</th>`;
    });
    html += '</tr></thead><tbody>';

    filteredRequests.forEach((item, idx) => {
      const bgColor = idx % 2 === 0 ? '#ffffff' : '#f5f5f5';
      html += `<tr style="background:${bgColor};">`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.id}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.is_form ? 'Форма' : 'Email'}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.email || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.fio || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.organization || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.phone || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.category || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.project || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.device_type || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.serial_number || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.inn || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.country_region || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;text-align:right;">${((item.confidence_score || 0) * 100).toFixed(1)}%</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${(item.user_message || '').substring(0, 50)}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${(item.ai_generated_answer || '').substring(0, 50)}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${(item.operator_answer || '').substring(0, 50)}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.operator_notes || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.status || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${item.operator?.username || item.operator?.name || ''}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${formatDate(item.created_at)}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${formatDate(item.updated_at)}</td>`;
      html += `<td style="padding:8px;border:1px solid #ddd;">${formatDate(item.responded_at)}</td>`;
      html += '</tr>';
    });
    html += '</tbody></table>';

    const blob = new Blob([html], { type: 'application/vnd.ms-excel;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `archive-${new Date().toISOString().split('T')[0]}.xls`;
    link.click();
  };

  const handleCloseModal = () => {
    setSelectedRequest(null);
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
          <div style={{ display: 'flex', gap: 8 }}>
            <button className="button secondary" onClick={loadArchive}>
              <RefreshCw size={16} /> Обновить
            </button>
            <button className="button secondary" onClick={exportToCSV} title="Экспорт в CSV">
              <FileText size={16} /> CSV
            </button>
            <button className="button secondary" onClick={exportToExcel} title="Экспорт в Excel">
              <FileSpreadsheet size={16} /> Excel
            </button>
          </div>
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
                setTempDateFrom('');
                setTempDateTo('');
              }}
            >
              Сбросить фильтры
            </button>
          </div>
        </div>

        <div className="grid cols-4" style={{ marginTop: 12 }}>
          <div>
            <label className="label">Дата от</label>
            <input className="input" type="date" value={tempDateFrom} onChange={(event) => setTempDateFrom(event.target.value)} />
          </div>
          <div>
            <label className="label">Дата до</label>
            <input className="input" type="date" value={tempDateTo} onChange={(event) => setTempDateTo(event.target.value)} />
          </div>
          <div style={{ display: 'flex', alignItems: 'end', gap: 8 }}>
            <button
              className="button"
              style={{ width: '100%' }}
              onClick={handleApplyDateFilter}
            >
              <Calendar size={16} /> Применить даты
            </button>
          </div>
        </div>
      </div>

      <div className="card" style={{ overflowX: 'auto' }}>
        <table className="table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Источник</th>
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
                <td colSpan={10} style={{ color: 'var(--ink-soft)' }}>
                  Загрузка...
                </td>
              </tr>
            ) : filteredRequests.length === 0 ? (
              <tr>
                <td colSpan={10} style={{ color: 'var(--ink-soft)' }}>
                  По текущим фильтрам ничего не найдено
                </td>
              </tr>
            ) : (
              paginatedRequests.map((item) => (
                <tr
                  key={item.id}
                  onClick={() => handleRowClick(item)}
                  style={{ cursor: 'pointer' }}
                  onMouseEnter={(e) => e.currentTarget.style.backgroundColor = 'rgba(40, 196, 161, 0.08)'}
                  onMouseLeave={(e) => e.currentTarget.style.backgroundColor = 'transparent'}
                >
                  <td>{item.id}</td>
                  <td>
                    <div style={{ display: 'flex', alignItems: 'center', gap: 6 }}>
                      {item.is_form ? <FileText size={14} /> : <Mail size={14} />}
                      <span className="tag" style={{ background: item.is_form ? 'rgba(100,150,255,0.15)' : 'rgba(255,150,100,0.15)', fontSize: '11px', padding: '2px 8px' }}>
                        {item.is_form ? 'Форма' : 'Email'}
                      </span>
                    </div>
                  </td>
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

      {selectedRequest && (
        <div
          style={{
            position: 'fixed',
            top: 0,
            left: 0,
            right: 0,
            bottom: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.75)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 1000,
            padding: 20
          }}
          onClick={handleCloseModal}
        >
          <div
            className="card"
            style={{
              maxWidth: 800,
              width: '100%',
              maxHeight: '90vh',
              overflow: 'auto',
              position: 'relative'
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 20 }}>
              <h2 style={{ margin: 0, display: 'flex', alignItems: 'center', gap: 10 }}>
                <Archive size={20} /> Заявка #{selectedRequest.id}
              </h2>
              <button
                className="button secondary"
                onClick={handleCloseModal}
                style={{ padding: '8px 12px' }}
              >
                <X size={16} />
              </button>
            </div>

            <div className="grid" style={{ gap: 16 }}>
              <div>
                <div className="label">Статус</div>
                <span className="tag" style={{ fontSize: 14, padding: '6px 14px' }}>{selectedRequest.status}</span>
              </div>
              
              <div>
                <div className="label">Источник заявки</div>
                <div style={{ display: 'flex', alignItems: 'center', gap: 8 }}>
                  {selectedRequest.is_form ? <FileText size={18} /> : <Mail size={18} />}
                  <span className="tag" style={{ 
                    fontSize: 14, 
                    padding: '6px 14px',
                    background: selectedRequest.is_form ? 'rgba(100,150,255,0.2)' : 'rgba(255,150,100,0.2)' 
                  }}>
                    {selectedRequest.is_form ? '📋 Форма' : '✉ Email'}
                  </span>
                </div>
              </div>

              <div className="grid cols-2">
                <div>
                  <div className="label">Email клиента</div>
                  <div style={{ fontSize: 16 }}>{selectedRequest.email || '—'}</div>
                </div>
                <div>
                  <div className="label">Категория</div>
                  <span className="tag">{selectedRequest.category || '—'}</span>
                </div>
              </div>

              <div className="grid cols-2">
                <div>
                  <div className="label">ФИО клиента</div>
                  <div style={{ fontSize: 16 }}>{selectedRequest.fio || '—'}</div>
                </div>
                <div>
                  <div className="label">Организация</div>
                  <div style={{ fontSize: 16 }}>{selectedRequest.organization || '—'}</div>
                </div>
              </div>

              <div className="grid cols-2">
                <div>
                  <div className="label">Телефон</div>
                  <div style={{ fontSize: 16 }}>{selectedRequest.phone || '—'}</div>
                </div>
                <div>
                  <div className="label">Проект</div>
                  <div style={{ fontSize: 16 }}>{selectedRequest.project || '—'}</div>
                </div>
              </div>

              <div className="grid cols-2">
                <div>
                  <div className="label">Тип устройства</div>
                  <div style={{ fontSize: 16 }}>{selectedRequest.device_type || '—'}</div>
                </div>
                <div>
                  <div className="label">Серийный номер</div>
                  <div style={{ fontSize: 16, fontFamily: 'var(--mono)' }}>{selectedRequest.serial_number || '—'}</div>
                </div>
              </div>

              <div className="grid cols-2">
                <div>
                  <div className="label">ИНН организации</div>
                  <div style={{ fontSize: 16, fontFamily: 'var(--mono)' }}>{selectedRequest.inn || '—'}</div>
                </div>
                <div>
                  <div className="label">Страна / Регион</div>
                  <div style={{ fontSize: 16 }}>{selectedRequest.country_region || '—'}</div>
                </div>
              </div>

              <div>
                <div className="label">Уверенность AI</div>
                <div style={{ fontSize: 20, fontWeight: 600, color: 'var(--accent)' }}>
                  {((selectedRequest.confidence_score || 0) * 100).toFixed(1)}%
                </div>
              </div>

              <div>
                <div className="label">Ответ, сгенерированный AI</div>
                <div
                  style={{
                    background: 'var(--panel-soft)',
                    borderRadius: 12,
                    padding: 14,
                    lineHeight: 1.6,
                    whiteSpace: 'pre-wrap'
                  }}
                >
                  {selectedRequest.ai_generated_answer || '—'}
                </div>
              </div>

              <div>
                <div className="label">Финальный ответ оператора</div>
                <div
                  style={{
                    background: 'var(--panel-soft)',
                    borderRadius: 12,
                    padding: 14,
                    lineHeight: 1.6,
                    whiteSpace: 'pre-wrap',
                    border: '1px solid rgba(40, 196, 161, 0.3)'
                  }}
                >
                  {selectedRequest.operator_answer || '—'}
                </div>
              </div>

              {selectedRequest.operator && (
                <div>
                  <div className="label">Оператор</div>
                  <div style={{ fontSize: 16 }}>{selectedRequest.operator.name || selectedRequest.operator.username}</div>
                </div>
              )}

              <div className="grid cols-3">
                <div>
                  <div className="label">Создано</div>
                  <div style={{ fontSize: 14 }}>{formatDate(selectedRequest.created_at)}</div>
                </div>
                <div>
                  <div className="label">Обновлено</div>
                  <div style={{ fontSize: 14 }}>{formatDate(selectedRequest.updated_at)}</div>
                </div>
                <div>
                  <div className="label">Отправлено</div>
                  <div style={{ fontSize: 14 }}>{formatDate(selectedRequest.responded_at)}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
