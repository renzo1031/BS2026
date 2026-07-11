<script setup lang="ts">
const steps = [
  { no: '01', title: '个案建档', description: '授权工作人员记录基础情况，敏感字段加密保存。' },
  { no: '02', title: '需求审核', description: '部门主管核对必要性、公开摘要和服务边界。' },
  { no: '03', title: '志愿匹配', description: '认证志愿者申请，由主管选择并生成唯一服务任务。' },
  { no: '04', title: '服务结案', description: '回访、完成验收和评价形成可追踪闭环。' },
]
</script>

<template>
  <section id="process" class="process-band">
    <div class="container process-inner">
      <div class="process-heading">
        <p>工作流程</p>
        <h2>每一步都有责任人、状态和记录</h2>
      </div>
      <ol class="process-list">
        <li v-for="step in steps" :key="step.no">
          <span>{{ step.no }}</span>
          <div>
            <h3>{{ step.title }}</h3>
            <p>{{ step.description }}</p>
          </div>
        </li>
      </ol>
    </div>
  </section>
</template>

<style scoped>
.process-band {
  padding: 86px 0;
  color: #eef7f4;
  background: #17352e;
}

.process-inner {
  display: grid;
  grid-template-columns: 0.8fr 1.6fr;
  gap: 70px;
}

.process-heading p {
  margin: 0 0 10px;
  color: #7ad5ab;
  font-weight: 700;
}

.process-heading h2 {
  margin: 0;
  font-size: 34px;
  line-height: 1.45;
}

.process-list {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0;
  margin: 0;
  padding: 0;
  list-style: none;
  border-top: 1px solid #416158;
}

.process-list li {
  display: flex;
  gap: 18px;
  min-height: 164px;
  padding: 26px 24px 24px 0;
  border-bottom: 1px solid #416158;
}

.process-list li:nth-child(odd) {
  border-right: 1px solid #416158;
}

.process-list li:nth-child(even) {
  padding-left: 24px;
}

.process-list > li > span {
  color: #7ad5ab;
  font-size: 18px;
  font-weight: 700;
}

.process-list h3 {
  margin: 0 0 8px;
  color: #ffffff;
  font-size: 19px;
}

.process-list p {
  margin: 0;
  color: #b8cbc5;
  line-height: 1.8;
}

@media (max-width: 820px) {
  .process-inner {
    grid-template-columns: 1fr;
    gap: 38px;
  }
}

@media (max-width: 560px) {
  .process-list {
    grid-template-columns: 1fr;
  }

  .process-list li,
  .process-list li:nth-child(even) {
    padding: 24px 0;
    border-right: 0;
  }
}
</style>
