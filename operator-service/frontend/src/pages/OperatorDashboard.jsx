import { useEffect, useMemo, useState } from 'react';
import { CheckCircle, PencilLine } from 'lucide-react';
import { operatorApi } from '../api/client.js';

export default function OperatorDashboard() {
  const [requests, setRequests] = useState([]);
  const [selectedId, setSelectedId] = useState(null);
  const [draft, setDraft] = useState('');
  const [notes, setNotes] = useState('');
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    loadPendingRequests();
  }, []);

  const selected = useMemo(
    () => requests.find((item) => item.id === selectedId),
    [requests, selectedId]
  );

  const loadPendingRequests = async () => {
    try {
      setLoading(true);
      const data = await operatorApi.getPendingRequests();
      setRequests(data);
      if (data.length > 0) {
        setSelectedId(data[0].id);
        setDraft(data[0].operator_answer || data[0].ai_generated_answer || '');
      } else {
        setSelectedId(null);
        setDraft('');
      }
    } catch (error) {
      alert(error.response?.data?.message || 'Не удалось загрузить заявки');
    } finally {
      setLoading(false);
    }
  };

  const handleSelect = (item) => {
    setSelectedId(item.id);
    setDraft(item.operator_answer || item.ai_generated_answer || '');
    setNotes('');
  };

  const handleApprove = async () => {
    if (!selected) return;
    try {
      await operatorApi.approve(selected.id);
      await loadPendingRequests();
      alert('Ответ одобрен и отправлен клиенту');
    } catch (error) {
      alert(error.response?.data?.message || 'Не удалось одобрить и отправить ответ');
    }
  };

  const handleEditSend = async () => {
    if (!selected || !draft.trim()) return;
    try {
      await operatorApi.updateRequest(selected.id, {
         operator_answer: draft,
         operator_notes: notes
      });
      await operatorApi.sendResponse(selected.id);
      await loadPendingRequests();
      alert('Отредактированный ответ отправлен клиенту');
    } catch (error) {
      alert(error.response?.data?.message || 'Не удалось отправить ответ');
    }
  };

  const handleCreateMockRequest = async () => {
    try {
      await operatorApi.createRequest({
        id: `REQ-${Date.now()}`,
        email: 'jesusdangerous@yandex.ru',
        organization: 'ООО Живая Сталь',
        fio: 'Калашников Владислав Сергеевич',
        phone: '+79826139545',
        device_type: 'газотурбинное оборудование',
        serial_number: '123123',
        category: 'Обращение по продукции',
        project: 'Тестовый проект по эксплуатации',
        inn: null,
        country_region: 'Россия',
        file: [84, 69, 83, 84],
        confidence_score: 0.55,
        status: 'new',
        ai_generated_answer:
          'Здравствуйте! Для корректной эксплуатации газотурбинного оборудования рекомендуем следовать инструкции производителя и регламенту технического обслуживания.',
        operator_answer:
          'Здравствуйте! Для корректной эксплуатации газотурбинного оборудования рекомендуем следовать инструкции производителя и регламенту технического обслуживания.',
        created_at: new Date().toISOString(),
        updated_at: new Date().toISOString()
      });
      await loadPendingRequests();
      alert('Тестовая заявка полного формата создана');
    } catch (error) {
      alert(error.response?.data?.message || 'Не удалось создать тестовую заявку');
    }
  };

  return (
    <div className="grid" style={{ gridTemplateColumns: '320px 1fr', gap: 20 }}>
      <div className="card" style={{ height: 'calc(100vh - 120px)', overflow: 'auto' }}>
        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'baseline' }}>
          <h3 style={{ margin: 0 }}>Заявки в ожидании</h3>
          <span className="tag">{loading ? '...' : `${requests.length} шт.`}</span>
        </div>

        <button
          className="button secondary"
          style={{ marginTop: 12, width: '100%' }}
          onClick={handleCreateMockRequest}
        >
          Создать тестовую заявку
        </button>

        <button
          className="button secondary"
          style={{ marginTop: 8, width: '100%' }}
          onClick={loadPendingRequests}
        >
          Обновить список
        </button>

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
              <div style={{ fontSize: 13, color: 'var(--ink-soft)' }}>{item.email}</div>
              <div style={{ fontWeight: 600 }}>{item.project || item.organization || 'Без проекта'}</div>
              <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
                <span className="tag">{item.category}</span>
                <span className="tag">AI {((item.confidence_score || 0) * 100).toFixed(0)}%</span>
              </div>
            </button>
          ))}
        </div>
      </div>

      <div>
        {!selected ? (
          <div className="card">Выберите заявку для просмотра.</div>
        ) : (
          <>
            <div className="card">
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <div>
                  <div className="label">Отправитель</div>
                  <div>{selected.email}</div>
                </div>
                <div>
                  <div className="label">Категория</div>
                  <div>{selected.category}</div>
                </div>
              </div>
              <div style={{ marginTop: 16 }}>
                <div className="label">Клиент</div>
                <div style={{ fontSize: 18, fontWeight: 600 }}>{selected.fio || 'Не указано'}</div>
              </div>
              <div style={{ marginTop: 16 }}>
                <div className="label">Обращение</div>
                <div style={{ color: 'var(--ink-soft)', lineHeight: 1.6 }}>
                  Организация: {selected.organization || '—'}
                  <br />
                  Тип прибора: {selected.device_type || '—'}
                  <br />
                  Серийный номер: {selected.serial_number || '—'}
                  <br />
                  Телефон: {selected.phone || '—'}
                </div>
              </div>
            </div>

            <div className="card">
              <div className="label">Черновик AI</div>
              <div style={{ background: '#10161d', borderRadius: 12, padding: 14 }}>
                {selected.ai_generated_answer}
              </div>
              <div style={{ marginTop: 16, display: 'flex', gap: 12 }}>
                <button className="button" onClick={handleApprove}>
                  <CheckCircle size={16} /> Одобрить и отправить
                </button>
              </div>
            </div>

            <div className="card">
              <div style={{ display: 'flex', alignItems: 'center', gap: 10 }}>
                <PencilLine size={18} />
                <h3 style={{ margin: 0 }}>Редактировать ответ</h3>
              </div>
              <div style={{ marginTop: 12 }}>
                <label className="label">Ответ</label>
                <textarea
                  className="textarea"
                  rows={6}
                  value={draft}
                  onChange={(event) => setDraft(event.target.value)}
                />
              </div>
              <div style={{ marginTop: 12 }}>
                <label className="label">Заметки (внутренние)</label>
                <textarea
                  className="textarea"
                  rows={3}
                  value={notes}
                  onChange={(event) => setNotes(event.target.value)}
                />
              </div>
              <button className="button" style={{ marginTop: 12 }} onClick={handleEditSend}>
                Редактировать и отправить
              </button>
            </div>
          </>
        )}
      </div>
    </div>
  );
}
