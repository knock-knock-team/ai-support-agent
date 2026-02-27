import { useEffect, useMemo, useState } from 'react';
import { BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer, PieChart, Pie, Cell, LineChart, Line, Legend } from 'recharts';
import { Download, FileText, FileSpreadsheet } from 'lucide-react';
import { operatorApi } from '../api/client.js';

const palette = ['#28c4a1', '#f59e0b', '#60a5fa', '#ef4444'];
const PERIOD_OPTIONS = [
  { label: '7 дней', value: 7 },
  { label: '30 дней', value: 30 },
  { label: '90 дней', value: 90 }
];

export default function Dashboard() {
  const [analytics, setAnalytics] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  const [selectedPeriod, setSelectedPeriod] = useState(30);

  useEffect(() => {
    let isMounted = true;

    const loadAnalytics = async () => {
      try {
        setLoading(true);
        setError('');
        const data = await operatorApi.getDashboardAnalytics(selectedPeriod);
        if (isMounted) {
          setAnalytics(data);
        }
      } catch (requestError) {
        if (isMounted) {
          setError('Не удалось загрузить аналитику');
        }
      } finally {
        if (isMounted) {
          setLoading(false);
        }
      }
    };

    loadAnalytics();

    return () => {
      isMounted = false;
    };
  }, [selectedPeriod]);

  const summary = analytics?.summary ?? { total: 0, pending: 0, approved: 0, edited: 0 };
  const byCategory = analytics?.byCategory ?? [];
  const byStatus = analytics?.byStatus ?? [];
  const timeSeriesData = analytics?.timeSeries ?? [];
  const detailsByCategory = analytics?.detailsByCategory ?? [];

  const totals = useMemo(
    () => [
      { label: 'Всего', value: summary.total },
      { label: 'В ожидании', value: summary.pending },
      { label: 'Одобрено', value: summary.approved },
      { label: 'Отредактировано', value: summary.edited }
    ],
    [summary]
  );

  const exportToCSV = () => {
    const headers = ['Категория,Значение'];
    const categoryRows = byCategory.map(item => `${item.name},${item.value}`);
    const statusRows = byStatus.map(item => `${item.name},${item.value}`);
    
    const csv = [
      'Статистика по категориям',
      ...headers,
      ...categoryRows,
      '',
      'Статистика по статусам',
      ...headers,
      ...statusRows
    ].join('\n');

    const blob = new Blob(['\ufeff' + csv], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `dashboard-${new Date().toISOString().split('T')[0]}.csv`;
    link.click();
  };

  const exportToJSON = () => {
    const data = {
      summary: {
        total: summary.total,
        pending: summary.pending,
        approved: summary.approved,
        edited: summary.edited
      },
      byCategory,
      byStatus,
      timeSeries: timeSeriesData,
      detailsByCategory,
      exportDate: new Date().toISOString()
    };

    const json = JSON.stringify(data, null, 2);
    const blob = new Blob([json], { type: 'application/json' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `dashboard-${new Date().toISOString().split('T')[0]}.json`;
    link.click();
  };

  const exportToPDF = () => {
    // Простой HTML отчет (можно улучшить с помощью библиотеки типа jsPDF)
    const html = `
      <!DOCTYPE html>
      <html>
      <head>
        <meta charset="UTF-8">
        <title>Отчет по дашборду</title>
        <style>
          body { font-family: Arial, sans-serif; padding: 40px; }
          h1 { color: #333; }
          table { width: 100%; border-collapse: collapse; margin: 20px 0; }
          th, td { border: 1px solid #ddd; padding: 12px; text-align: left; }
          th { background-color: #28c4a1; color: white; }
          .summary { display: grid; grid-template-columns: repeat(4, 1fr); gap: 20px; margin: 20px 0; }
          .card { padding: 20px; background: #f5f5f5; border-radius: 8px; }
        </style>
      </head>
      <body>
        <h1>Статистика заявок</h1>
        <p>Дата: ${new Date().toLocaleDateString('ru-RU')}</p>
        
        <div class="summary">
          <div class="card"><h3>Всего</h3><p style="font-size: 24px;">${summary.total}</p></div>
          <div class="card"><h3>В ожидании</h3><p style="font-size: 24px;">${summary.pending}</p></div>
          <div class="card"><h3>Одобрено</h3><p style="font-size: 24px;">${summary.approved}</p></div>
          <div class="card"><h3>Отредактировано</h3><p style="font-size: 24px;">${summary.edited}</p></div>
        </div>

        <h2>По категориям</h2>
        <table>
          <thead><tr><th>Категория</th><th>Количество</th></tr></thead>
          <tbody>
            ${byCategory.map(item => `<tr><td>${item.name}</td><td>${item.value}</td></tr>`).join('')}
          </tbody>
        </table>

        <h2>По статусам</h2>
        <table>
          <thead><tr><th>Статус</th><th>Количество</th></tr></thead>
          <tbody>
            ${byStatus.map(item => `<tr><td>${item.name}</td><td>${item.value}</td></tr>`).join('')}
          </tbody>
        </table>
      </body>
      </html>
    `;

    const blob = new Blob([html], { type: 'text/html' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `dashboard-${new Date().toISOString().split('T')[0]}.html`;
    link.click();
  };

  if (loading) {
    return (
      <div className="card">
        <h3 style={{ margin: 0 }}>Панель аналитики</h3>
        <p style={{ marginTop: 8, color: 'var(--ink-soft)' }}>Загрузка данных...</p>
      </div>
    );
  }

  if (error) {
    return (
      <div className="card">
        <h3 style={{ margin: 0 }}>Панель аналитики</h3>
        <p style={{ marginTop: 8, color: 'var(--danger)' }}>{error}</p>
      </div>
    );
  }

  return (
    <div className="grid" style={{ gap: 20 }}>
      {/* Заголовок с кнопками экспорта */}
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', gap: 16, flexWrap: 'wrap' }}>
        <div>
          <h2 style={{ margin: 0 }}>Панель аналитики</h2>
          <p style={{ color: 'var(--ink-soft)', margin: '4px 0 0 0' }}>
            Статистика и метрики обработки заявок за выбранный период
          </p>
        </div>

        <div style={{ display: 'flex', alignItems: 'center', gap: 8, flexWrap: 'wrap' }}>
          {PERIOD_OPTIONS.map((option) => (
            <button
              key={option.value}
              className={selectedPeriod === option.value ? 'button' : 'button secondary'}
              onClick={() => setSelectedPeriod(option.value)}
              type="button"
            >
              {option.label}
            </button>
          ))}
        </div>

        <div style={{ display: 'flex', gap: 12, flexWrap: 'wrap' }}>
          <button className="button secondary" onClick={exportToCSV} title="Экспорт в CSV">
            <FileSpreadsheet size={16} /> CSV
          </button>
          <button className="button secondary" onClick={exportToJSON} title="Экспорт в JSON">
            <FileText size={16} /> JSON
          </button>
          <button className="button secondary" onClick={exportToPDF} title="Экспорт в HTML">
            <Download size={16} /> HTML
          </button>
        </div>
      </div>

      {/* Карточки со статистикой */}
      <div className="grid cols-4">
        {totals.map((item) => (
          <div key={item.label} className="card">
            <div className="label">{item.label}</div>
            <div style={{ fontSize: 28, fontWeight: 700 }}>{item.value}</div>
          </div>
        ))}
      </div>

      {/* График динамики */}
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Динамика заявок</h3>
        <div style={{ height: 260 }}>
          <ResponsiveContainer width="100%" height="100%">
            <LineChart data={timeSeriesData}>
              <XAxis dataKey="date" stroke="#b6c2d0" />
              <YAxis stroke="#b6c2d0" />
              <Tooltip />
              <Legend />
              <Line type="monotone" dataKey="requests" name="Заявки" stroke="#28c4a1" strokeWidth={2} />
            </LineChart>
          </ResponsiveContainer>
        </div>
      </div>

      {/* Графики по категориям и статусам */}
      <div className="grid cols-2">
        <div className="card">
          <h3 style={{ marginTop: 0 }}>Заявки по статусу</h3>
          <div style={{ height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie data={byStatus} dataKey="value" nameKey="name" innerRadius={60} outerRadius={100}>
                  {byStatus.map((entry, index) => (
                    <Cell key={entry.name} fill={palette[index % palette.length]} />
                  ))}
                </Pie>
                <Tooltip />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>

        <div className="card">
          <h3 style={{ marginTop: 0 }}>Заявки по категориям</h3>
          <div style={{ height: 260 }}>
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={byCategory}>
                <XAxis dataKey="name" stroke="#b6c2d0" />
                <YAxis stroke="#b6c2d0" />
                <Tooltip />
                <Bar dataKey="value" fill="#28c4a1" radius={[8, 8, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Таблица с детальной статистикой */}
      <div className="card">
        <h3 style={{ marginTop: 0 }}>Детальная статистика</h3>
        <table className="table" style={{ marginTop: 12 }}>
          <thead>
            <tr>
              <th>Категория</th>
              <th>Всего</th>
              <th>В ожидании</th>
              <th>Обработано</th>
              <th>% обработки</th>
            </tr>
          </thead>
          <tbody>
            {detailsByCategory.map((cat) => {
              const percentage = Number(cat.processingRate ?? 0);
              
              return (
                <tr key={cat.category}>
                  <td>{cat.category}</td>
                  <td>{cat.total}</td>
                  <td>{cat.pending}</td>
                  <td>{cat.processed}</td>
                  <td>
                    <span style={{ 
                      color: percentage > 80 ? 'var(--accent)' : 'var(--warning)',
                      fontWeight: 600 
                    }}>
                      {percentage.toFixed(1)}%
                    </span>
                  </td>
                </tr>
              );
            })}
          </tbody>
        </table>
      </div>
    </div>
  );
}
