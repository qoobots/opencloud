<template>
  <div class="metric-card" :class="[`color-${color}`, { clickable }]" @click="$emit('click')">
    <div class="card-icon">
      <el-icon :size="28"><component :is="icon" /></el-icon>
    </div>
    <div class="card-body">
      <div class="card-value">
        <span class="value-num">{{ displayValue }}</span>
        <span v-if="unit" class="value-unit">{{ unit }}</span>
        <span v-if="trend !== undefined" class="trend" :class="trend >= 0 ? 'up' : 'down'">
          <el-icon><component :is="trend >= 0 ? 'ArrowUp' : 'ArrowDown'" /></el-icon>
          {{ Math.abs(trend) }}%
        </span>
      </div>
      <div class="card-label">{{ label }}</div>
      <div v-if="desc" class="card-desc">{{ desc }}</div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = withDefaults(defineProps<{
  label: string
  value: number | string
  unit?: string
  icon?: string
  color?: 'blue' | 'green' | 'orange' | 'red' | 'purple' | 'teal'
  trend?: number
  desc?: string
  clickable?: boolean
  loading?: boolean
}>(), {
  icon: 'DataLine',
  color: 'blue',
  clickable: false,
})

defineEmits<{ click: [] }>()

const displayValue = computed(() => {
  if (props.loading) return '...'
  return props.value
})
</script>

<style scoped>
.metric-card {
  display: flex;
  align-items: center;
  gap: 16px;
  background: #fff;
  border-radius: 10px;
  padding: 20px 24px;
  box-shadow: 0 2px 8px rgba(0,0,0,.06);
  transition: box-shadow .2s, transform .2s;
  border-left: 4px solid var(--card-color);
}

.metric-card.clickable { cursor: pointer; }
.metric-card.clickable:hover {
  box-shadow: 0 4px 16px rgba(0,0,0,.12);
  transform: translateY(-2px);
}

.card-icon {
  width: 56px;
  height: 56px;
  border-radius: 12px;
  background: color-mix(in srgb, var(--card-color) 12%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--card-color);
  flex-shrink: 0;
}

.card-body { flex: 1; min-width: 0; }

.card-value {
  display: flex;
  align-items: baseline;
  gap: 6px;
  flex-wrap: wrap;
}

.value-num {
  font-size: 28px;
  font-weight: 700;
  color: #1d2b3a;
  line-height: 1;
}

.value-unit { font-size: 13px; color: #909399; }

.trend {
  display: flex;
  align-items: center;
  gap: 2px;
  font-size: 12px;
  padding: 2px 6px;
  border-radius: 4px;
}
.trend.up   { color: #67c23a; background: #f0f9eb; }
.trend.down { color: #f56c6c; background: #fef0f0; }

.card-label { font-size: 13px; color: #606266; margin-top: 6px; }
.card-desc  { font-size: 12px; color: #c0c4cc; margin-top: 2px; }

/* 颜色主题 */
.color-blue   { --card-color: #409eff; }
.color-green  { --card-color: #67c23a; }
.color-orange { --card-color: #e6a23c; }
.color-red    { --card-color: #f56c6c; }
.color-purple { --card-color: #9c8fe6; }
.color-teal   { --card-color: #26b5a8; }
</style>
