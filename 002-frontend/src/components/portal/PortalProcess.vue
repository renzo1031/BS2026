<script setup>
const steps = [
  { title: '选择事项', description: '确认办理范围和所需信息' },
  { title: '填写申请', description: '在线提交本人业务资料' },
  { title: '部门办理', description: '对应部门受理并处理' },
  { title: '查看结果', description: '接收通知、下载结果或评价' }
]
</script>

<template>
  <section class="process-band" aria-labelledby="process-title">
    <div class="process-inner">
      <header class="process-heading">
        <h2 id="process-title">网上办理流程</h2>
        <p>每一步都有状态记录，办理进度可随时查看。</p>
      </header>
      <ol class="process-list">
        <li v-for="(step, index) in steps" :key="step.title">
          <span class="process-index">{{ index + 1 }}</span>
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
  color: #eef4f1;
  background: #173f37;
}

.process-inner {
  width: min(1180px, calc(100% - 40px));
  margin: 0 auto;
  padding: 54px 0 58px;
}

.process-heading {
  display: grid;
  grid-template-columns: minmax(220px, 0.8fr) minmax(280px, 1.2fr);
  gap: 40px;
  align-items: end;
}

.process-heading h2 {
  margin: 0;
  font-family: "Songti SC", STSong, "Microsoft YaHei", serif;
  font-size: 30px;
}

.process-heading p {
  margin: 0;
  color: #b9cbc5;
  line-height: 1.7;
}

.process-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 0;
  margin: 40px 0 0;
  padding: 0;
  list-style: none;
}

.process-list li {
  position: relative;
  display: grid;
  grid-template-columns: 38px minmax(0, 1fr);
  gap: 14px;
  min-height: 82px;
  padding: 0 24px;
  border-left: 1px solid #3f655d;
}

.process-list li:first-child {
  padding-left: 0;
  border-left: 0;
}

.process-index {
  width: 34px;
  height: 34px;
  display: grid;
  place-items: center;
  border: 1px solid #78958d;
  border-radius: 50%;
  color: #fff;
  font-weight: 700;
}

.process-list h3 {
  margin: 2px 0 7px;
  font-size: 16px;
}

.process-list p {
  margin: 0;
  color: #aec1bb;
  font-size: 13px;
  line-height: 1.55;
}

@media (max-width: 800px) {
  .process-inner {
    width: calc(100% - 28px);
    padding: 46px 0 50px;
  }

  .process-heading {
    grid-template-columns: 1fr;
    gap: 10px;
  }

  .process-list {
    grid-template-columns: 1fr 1fr;
    gap: 26px 0;
    margin-top: 32px;
  }

  .process-list li,
  .process-list li:first-child {
    padding: 0 18px;
    border-left: 1px solid #3f655d;
  }
}

@media (max-width: 480px) {
  .process-list {
    grid-template-columns: 1fr;
  }

  .process-list li,
  .process-list li:first-child {
    padding-left: 0;
    border-left: 0;
  }
}
</style>
