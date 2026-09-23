import { DragDropContext, Draggable, Droppable } from '@hello-pangea/dnd';
import clsx from 'clsx';
import { ArrowLeft, Filter, Search, Star } from 'lucide-react';
import { useCallback, useEffect, useState } from 'react';
import toast from 'react-hot-toast';
import { Link, useParams } from 'react-router-dom';
import { errorMessage } from '../../api/client';
import { recruiterApi } from '../../api/services';
import { Avatar, PageLoader } from '../../components/ui';
import { STAGE_META, scoreColor, timeAgo } from '../../utils/format';
import CandidateDrawer from './CandidateDrawer';

export default function KanbanBoard() {
  const { jobId } = useParams();
  const [board, setBoard] = useState(null);
  const [dragging, setDragging] = useState(null);
  const [selected, setSelected] = useState(null);
  const [filters, setFilters] = useState({ q: '', minAts: '' });

  const load = useCallback(() => {
    const params = { q: filters.q || undefined, minAts: filters.minAts || undefined };
    return recruiterApi.board(jobId, params).then(setBoard).catch((e) => toast.error(errorMessage(e)));
  }, [jobId, filters]);

  useEffect(() => {
    const t = setTimeout(load, 250); // debounce filter typing
    return () => clearTimeout(t);
  }, [load]);

  const findCard = (id) => board.columns.flatMap((c) => c.cards).find((c) => String(c.id) === id);

  const onDragEnd = async ({ source, destination, draggableId }) => {
    setDragging(null);
    if (!destination) return;
    const from = source.droppableId;
    const to = destination.droppableId;
    if (from === to && source.index === destination.index) return;
    const card = findCard(draggableId);
    if (from !== to && !card.allowedTransitions.includes(to)) {
      toast.error(`Can't move from ${STAGE_META[from].label} to ${STAGE_META[to].label}`);
      return;
    }

    // Optimistic update so the drag feels instant; roll back on failure.
    const previous = board;
    const columns = board.columns.map((c) => ({ ...c, cards: [...c.cards] }));
    const src = columns.find((c) => c.stage === from);
    const dst = columns.find((c) => c.stage === to);
    const [moved] = src.cards.splice(source.index, 1);
    dst.cards.splice(destination.index, 0, { ...moved, allowedTransitions: moved.allowedTransitions });
    const counts = { ...board.counts };
    if (from !== to) { counts[from] -= 1; counts[to] += 1; }
    setBoard({ ...board, columns, counts });

    try {
      const updated = await recruiterApi.moveStage(card.id, { stage: to, position: destination.index });
      setBoard((b) => ({
        ...b,
        columns: b.columns.map((c) => ({ ...c, cards: c.cards.map((x) => (x.id === updated.id ? updated : x)) })),
      }));
      if (from !== to) toast.success(`${card.candidateName} → ${STAGE_META[to].label}`);
    } catch (e) {
      setBoard(previous);
      toast.error(errorMessage(e));
    }
  };

  if (!board) return <PageLoader />;
  const draggingCard = dragging && findCard(dragging);

  return (
    <div className="flex h-full flex-col space-y-5">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <Link to="/recruiter/jobs" className="mb-1 inline-flex items-center gap-1 text-sm text-slate-500 hover:text-slate-700">
            <ArrowLeft className="h-4 w-4" /> All jobs
          </Link>
          <h1 className="page-title">{board.job.title}</h1>
          <p className="text-sm text-slate-500">{board.job.applicantCount} applicants · drag cards to move them through the pipeline</p>
        </div>
        <div className="flex flex-wrap gap-2">
          <div className="relative">
            <Search className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <input className="input w-56 pl-9" placeholder="Name or skill" value={filters.q}
              onChange={(e) => setFilters({ ...filters, q: e.target.value })} />
          </div>
          <div className="relative">
            <Filter className="absolute left-3 top-2.5 h-4 w-4 text-slate-400" />
            <select className="input w-44 pl-9" value={filters.minAts} onChange={(e) => setFilters({ ...filters, minAts: e.target.value })}>
              <option value="">Any ATS score</option>
              <option value="50">ATS ≥ 50%</option>
              <option value="65">ATS ≥ 65%</option>
              <option value="80">ATS ≥ 80%</option>
            </select>
          </div>
        </div>
      </div>

      <DragDropContext onDragStart={(s) => setDragging(s.draggableId)} onDragEnd={onDragEnd}>
        <div className="flex flex-1 gap-4 overflow-x-auto pb-4">
          {board.columns.map((col) => {
            const meta = STAGE_META[col.stage];
            const blocked = draggingCard && col.stage !== draggingCard.stage && !draggingCard.allowedTransitions.includes(col.stage);
            return (
              <div key={col.stage} className={clsx('flex w-64 shrink-0 flex-col rounded-xl border-t-4 bg-slate-100/80 transition', meta.bar, blocked && 'opacity-40')}>
                <div className="flex items-center justify-between px-3 py-3">
                  <span className="text-xs font-bold uppercase tracking-wide text-slate-600">{meta.label}</span>
                  <span className="chip bg-white text-slate-600">{board.counts[col.stage]}</span>
                </div>
                <Droppable droppableId={col.stage} isDropDisabled={!!blocked}>
                  {(provided, snapshot) => (
                    <div ref={provided.innerRef} {...provided.droppableProps}
                      className={clsx('min-h-[120px] flex-1 space-y-2 rounded-b-xl px-2 pb-2 transition', snapshot.isDraggingOver && 'bg-brand-50')}>
                      {col.cards.map((card, index) => (
                        <Draggable key={card.id} draggableId={String(card.id)} index={index}>
                          {(p, s) => (
                            <div ref={p.innerRef} {...p.draggableProps} {...p.dragHandleProps} onClick={() => setSelected(card.id)}
                              className={clsx('cursor-pointer rounded-lg border bg-white p-3 shadow-sm transition hover:border-brand-300',
                                s.isDragging && 'rotate-2 shadow-lg ring-2 ring-brand-400')}>
                              <div className="flex items-start justify-between gap-2">
                                <div className="flex min-w-0 items-center gap-2">
                                  <Avatar name={card.candidateName} className="h-7 w-7 text-[11px]" />
                                  <div className="min-w-0">
                                    <p className="truncate text-sm font-semibold">{card.candidateName}</p>
                                    <p className="truncate text-xs text-slate-500">{card.headline || card.candidateEmail}</p>
                                  </div>
                                </div>
                                <span className={clsx('chip font-bold', scoreColor(card.atsScore).bg, scoreColor(card.atsScore).text)}>{card.atsScore}%</span>
                              </div>
                              <div className="mt-2 flex flex-wrap gap-1">
                                {card.matchedSkills.slice(0, 3).map((s) => <span key={s} className="rounded bg-slate-100 px-1.5 py-0.5 text-[10px] text-slate-600">{s}</span>)}
                              </div>
                              <div className="mt-2 flex items-center justify-between text-[11px] text-slate-400">
                                <span>TT-{card.id}</span>
                                <span className="flex items-center gap-2">
                                  {card.rating && <span className="flex items-center text-amber-500"><Star className="h-3 w-3 fill-current" />{card.rating}</span>}
                                  {timeAgo(card.appliedAt)}
                                </span>
                              </div>
                            </div>
                          )}
                        </Draggable>
                      ))}
                      {provided.placeholder}
                    </div>
                  )}
                </Droppable>
              </div>
            );
          })}
        </div>
      </DragDropContext>

      <CandidateDrawer applicationId={selected} onClose={() => setSelected(null)} onChanged={load} />
    </div>
  );
}
