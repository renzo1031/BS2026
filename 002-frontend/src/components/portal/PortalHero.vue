<script setup>
import { shallowRef } from 'vue'
import { NIcon } from 'naive-ui'
import { SearchOutline } from '@vicons/ionicons5'

defineProps({
  userName: { type: String, default: '同学' }
})

const emit = defineEmits(['search'])
const query = shallowRef('')

function submitSearch() {
  emit('search', query.value.trim())
}
</script>

<template>
  <section class="portal-hero" aria-labelledby="portal-title">
    <div class="portal-hero-inner">
      <div class="portal-hero-copy">
        <p class="portal-kicker">{{ userName }}，欢迎回来</p>
        <h1 id="portal-title">大学生一体化<span class="mobile-title-break"><br /></span>服务平台</h1>
        <p class="portal-summary">报修、证明、场地申请，在线提交并随时查看办理进度。</p>
        <form class="portal-search" role="search" @submit.prevent="submitSearch">
          <n-icon class="portal-search-icon" :component="SearchOutline" />
          <label class="sr-only" for="portal-service-search">查找服务事项</label>
          <input
            id="portal-service-search"
            v-model="query"
            type="search"
            placeholder="输入事项名称，例如“在读证明”"
            autocomplete="off"
          />
          <button type="submit">查找事项</button>
        </form>
      </div>
    </div>
  </section>
</template>

<style scoped>
.portal-hero {
  position: relative;
  isolation: isolate;
  min-height: 440px;
  background-image: url('../../assets/campus-hall.jpg');
  background-position: center 52%;
  background-size: cover;
}

.portal-hero::before {
  content: "";
  position: absolute;
  inset: 0;
  z-index: -1;
  background: rgb(249 251 250 / 30%);
}

.portal-hero-inner {
  width: min(1240px, calc(100% - 40px));
  min-height: 440px;
  margin: 0 auto;
  padding-top: 62px;
}

.portal-hero-copy {
  width: min(650px, 100%);
  color: #173f37;
  text-shadow: 0 1px 8px rgb(255 255 255 / 84%);
}

.portal-kicker {
  margin: 0 0 10px;
  font-weight: 700;
  color: #8d332f;
}

.portal-hero h1 {
  max-width: 620px;
  margin: 0;
  font-family: "Songti SC", STSong, "Microsoft YaHei", serif;
  font-size: 46px;
  line-height: 1.18;
  letter-spacing: 0;
}

.mobile-title-break {
  display: none;
}

.portal-summary {
  margin: 18px 0 26px;
  font-size: 18px;
  line-height: 1.7;
  color: #2d4640;
}

.portal-search {
  width: min(590px, 100%);
  min-height: 58px;
  display: grid;
  grid-template-columns: 24px minmax(0, 1fr) auto;
  align-items: center;
  gap: 10px;
  padding: 6px 7px 6px 18px;
  background: #fff;
  border: 1px solid rgb(23 63 55 / 18%);
  border-radius: 6px;
  box-shadow: 0 14px 36px rgb(25 45 40 / 16%);
  text-shadow: none;
}

.portal-search-icon {
  color: #5d6b67;
  font-size: 20px;
}

.portal-search input {
  width: 100%;
  min-width: 0;
  height: 44px;
  border: 0;
  outline: 0;
  color: #22312d;
  background: transparent;
  font-size: 15px;
}

.portal-search input::placeholder {
  color: #87928e;
}

.portal-search:focus-within {
  border-color: #205c50;
  box-shadow: 0 0 0 3px rgb(32 92 80 / 16%), 0 14px 36px rgb(25 45 40 / 16%);
}

.portal-search button {
  height: 44px;
  padding: 0 22px;
  border: 0;
  border-radius: 4px;
  color: #fff;
  background: #a5413b;
  font-weight: 700;
  cursor: pointer;
}

.portal-search button:hover,
.portal-search button:focus-visible {
  background: #89332e;
}

@media (max-width: 700px) {
  .portal-hero,
  .portal-hero-inner {
    min-height: 400px;
  }

  .portal-hero {
    background-position: 58% center;
  }

  .portal-hero::before {
    background: rgb(249 251 250 / 48%);
  }

  .portal-hero-inner {
    width: calc(100% - 28px);
    padding-top: 44px;
  }

  .portal-hero h1 {
    max-width: 340px;
    font-size: 36px;
  }

  .mobile-title-break {
    display: inline;
  }

  .portal-summary {
    max-width: 340px;
    margin: 14px 0 22px;
    font-size: 16px;
  }

  .portal-search {
    min-height: 54px;
    grid-template-columns: 20px minmax(0, 1fr) auto;
    gap: 6px;
    padding-left: 12px;
  }

  .portal-search button {
    height: 40px;
    padding: 0 14px;
  }
}
</style>
