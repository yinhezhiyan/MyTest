<template>
  <div class="kg-panel">
    <div class="kg-header">
      <div>
        <h3>{{ title }}</h3>
        <div class="kg-subtitle" v-if="summary.studentName">
          {{ summary.studentName }}（{{ summary.username }}） · {{ subjectText }}
        </div>
      </div>
      <div class="kg-stats">
        <div class="stat-card"><span>知识点</span><strong>{{ summary.nodeCount || nodes.length }}</strong></div>
        <div class="stat-card"><span>关系边</span><strong>{{ summary.edgeCount || edges.length }}</strong></div>
        <div class="stat-card"><span>薄弱点</span><strong>{{ summary.weakNodeCount || 0 }}</strong></div>
        <div class="stat-card"><span>平均掌握度</span><strong>{{ formatPercent(summary.avgMastery || 0) }}</strong></div>
      </div>
    </div>

    <el-empty v-if="!nodes.length" description="暂无知识图谱数据" />
    <template v-else>
      <div class="legend-row">
        <span class="legend-item"><i class="dot weak"></i>薄弱</span>
        <span class="legend-item"><i class="dot learning"></i>学习中</span>
        <span class="legend-item"><i class="dot mastered"></i>已掌握</span>
        <span class="legend-item"><i class="dot unseen"></i>未激活</span>
      </div>

      <div class="graph-stage" :style="{ height: graphStageHeight + 'px' }">
        <svg class="graph-svg" :viewBox="`0 0 ${viewBoxWidth} ${graphStageHeight}`" preserveAspectRatio="xMidYMid meet">
          <line
            v-for="edge in placedEdges"
            :key="edge.id || `${edge.source}-${edge.target}-${edge.relationType}`"
            :x1="edge.from.x"
            :y1="edge.from.y"
            :x2="edge.to.x"
            :y2="edge.to.y"
            :stroke-width="Math.max(1.5, Number(edge.weight || 1) * 1.4)"
            :class="['graph-edge', edge.relationType || 'related']"
          />
        </svg>

        <div
          v-for="node in placedNodes"
          :key="node.id"
          class="graph-node"
          :class="node.statusClass"
          :style="{ left: `${node.left}px`, top: `${node.top}px`, width: nodeWidth + 'px' }"
        >
          <div class="node-title">{{ node.label }}</div>
          <div class="node-meta">掌握度 {{ formatPercent(node.mastery) }}</div>
          <div class="node-meta">答题 {{ node.totalTimes || 0 }} 次 · 错题 {{ node.wrongTimes || 0 }}</div>
        </div>
      </div>

      <div class="insight-grid">
        <div class="insight-card">
          <div class="insight-title">薄弱知识点 Top</div>
          <el-table :data="weakTop" size="small" empty-text="暂无薄弱知识点">
            <el-table-column prop="label" label="知识点" />
            <el-table-column label="薄弱度" width="100">
              <template #default="scope">{{ formatPercent(scope.row.weakness || 0) }}</template>
            </el-table-column>
            <el-table-column label="答题次数" width="100">
              <template #default="scope">{{ scope.row.totalTimes || 0 }}</template>
            </el-table-column>
          </el-table>
        </div>

        <div class="insight-card">
          <div class="insight-title">掌握较好知识点</div>
          <el-table :data="masteryTop" size="small" empty-text="暂无数据">
            <el-table-column prop="label" label="知识点" />
            <el-table-column label="掌握度" width="100">
              <template #default="scope">{{ formatPercent(scope.row.mastery || 0) }}</template>
            </el-table-column>
            <el-table-column label="章节" width="180">
              <template #default="scope">{{ (scope.row.chapters || []).slice(0, 2).join(' / ') || '-' }}</template>
            </el-table-column>
          </el-table>
        </div>
      </div>

      <div class="insight-card">
        <div class="insight-title">知识点明细</div>
        <el-table :data="nodes" size="small" max-height="320">
          <el-table-column prop="label" label="知识点" min-width="160" />
          <el-table-column label="状态" width="110">
            <template #default="scope">
              <el-tag :type="statusTagType(scope.row.status)">{{ statusLabel(scope.row.status) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="掌握度" width="100">
            <template #default="scope">{{ formatPercent(scope.row.mastery || 0) }}</template>
          </el-table-column>
          <el-table-column label="薄弱度" width="100">
            <template #default="scope">{{ formatPercent(scope.row.weakness || 0) }}</template>
          </el-table-column>
          <el-table-column label="章节" min-width="220">
            <template #default="scope">{{ (scope.row.chapters || []).join('、') || '-' }}</template>
          </el-table-column>
        </el-table>
      </div>
    </template>
  </div>
</template>

<script setup>
import { computed } from 'vue'

const props = defineProps({
  graphData: { type: Object, default: () => ({}) },
  title: { type: String, default: '知识图谱' }
})

const nodeWidth = 170
const rowHeight = 104
const viewBoxWidth = 980

const summary = computed(() => props.graphData?.summary || {})
const nodes = computed(() => props.graphData?.nodes || [])
const edges = computed(() => props.graphData?.edges || [])
const weakTop = computed(() => props.graphData?.weakTop || [])
const masteryTop = computed(() => props.graphData?.masteryTop || [])
const subjectText = computed(() => ({ DS: '数据结构', OS: '操作系统', CN: '计算机网络', CO: '计算机组成原理' })[summary.value.subject] || summary.value.subject || '')

const sortedNodes = computed(() => {
  const statusOrder = { WEAK: 0, LEARNING: 1, MASTERED: 2, SEEN: 3, UNSEEN: 4 }
  return [...nodes.value].sort((a, b) => {
    const statusGap = (statusOrder[a.status] ?? 9) - (statusOrder[b.status] ?? 9)
    if (statusGap !== 0) return statusGap
    return String(a.label || '').localeCompare(String(b.label || ''), 'zh-Hans-CN')
  })
})

const placedNodes = computed(() => {
  const maxCols = 4
  return sortedNodes.value.map((node, index) => {
    const col = index % maxCols
    const row = Math.floor(index / maxCols)
    const left = 40 + col * 230
    const top = 28 + row * rowHeight
    return {
      ...node,
      left,
      top,
      x: left + nodeWidth / 2,
      y: top + 36,
      statusClass: String(node.status || 'UNSEEN').toLowerCase()
    }
  })
})

const placedEdges = computed(() => {
  const indexMap = new Map(placedNodes.value.map(node => [node.id, node]))
  return edges.value
    .map(edge => ({
      ...edge,
      from: indexMap.get(edge.source),
      to: indexMap.get(edge.target)
    }))
    .filter(edge => edge.from && edge.to)
})

const graphStageHeight = computed(() => Math.max(420, Math.ceil(sortedNodes.value.length / 4) * rowHeight + 48))

const formatPercent = (value) => `${Math.round(Number(value || 0) * 100)}%`
const statusLabel = (status) => ({ WEAK: '薄弱', LEARNING: '学习中', MASTERED: '已掌握', SEEN: '已接触', UNSEEN: '未激活' }[status] || '未知')
const statusTagType = (status) => ({ WEAK: 'danger', LEARNING: 'warning', MASTERED: 'success', SEEN: '', UNSEEN: 'info' }[status] || 'info')
</script>

<style scoped>
.kg-panel{display:flex;flex-direction:column;gap:16px}
.kg-header{display:flex;justify-content:space-between;gap:16px;flex-wrap:wrap}
.kg-header h3{margin:0}
.kg-subtitle{color:#6b7280;font-size:13px;margin-top:4px}
.kg-stats{display:flex;gap:12px;flex-wrap:wrap}
.stat-card{min-width:112px;padding:10px 12px;border-radius:12px;background:#f8fafc;border:1px solid #e5e7eb;display:flex;flex-direction:column;gap:4px}
.stat-card span{font-size:12px;color:#6b7280}
.stat-card strong{font-size:20px;color:#111827}
.legend-row{display:flex;gap:16px;flex-wrap:wrap;font-size:13px;color:#475569}
.legend-item{display:flex;align-items:center;gap:6px}
.dot{width:10px;height:10px;border-radius:50%;display:inline-block}
.dot.weak{background:#ef4444}.dot.learning{background:#f59e0b}.dot.mastered{background:#10b981}.dot.unseen{background:#94a3b8}
.graph-stage{position:relative;overflow:auto;border-radius:16px;border:1px solid #e5e7eb;background:linear-gradient(180deg,#f8fbff 0%, #ffffff 100%)}
.graph-svg{position:absolute;inset:0;width:100%;height:100%}
.graph-edge{stroke:#cbd5e1;opacity:.95}
.graph-edge.prerequisite{stroke:#6366f1}
.graph-edge.related{stroke:#0ea5e9}
.graph-edge.contains{stroke:#10b981}
.graph-node{position:absolute;padding:10px 12px;border-radius:14px;border:1px solid #dbeafe;background:#fff;box-shadow:0 10px 22px rgba(15,23,42,.08)}
.graph-node.weak{border-color:#fecaca;background:#fff1f2}
.graph-node.learning{border-color:#fde68a;background:#fffbeb}
.graph-node.mastered{border-color:#a7f3d0;background:#ecfdf5}
.graph-node.seen,.graph-node.unseen{border-color:#cbd5e1;background:#f8fafc}
.node-title{font-size:14px;font-weight:700;color:#111827;margin-bottom:6px}
.node-meta{font-size:12px;color:#475569;line-height:1.5}
.insight-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:16px}
.insight-card{padding:14px;border-radius:14px;background:#fff;border:1px solid #e5e7eb}
.insight-title{font-size:15px;font-weight:700;color:#111827;margin-bottom:10px}
@media (max-width: 960px){.insight-grid{grid-template-columns:1fr}.graph-stage{height:520px}}
</style>
