<template>
  <div class="VuePagination" :class="props.theme.wrapper">
    <nav :class="props.theme.nav">
      <ul v-show="props.showPagination" :class="props.theme.list">
        <li
          v-if="props.hasEdgeNav"
          :class="props.theme.firstPage"
          @click="props.setFirstPage"
        >
          <a v-bind="{ ...props.aProps, ...props.firstPageProps }"><strong style="font-size: x-large">{{
            props.texts.first
          }}</strong></a>
        </li>

        <li
          v-if="props.hasChunksNav"
          :class="props.theme.prevChunk"
          @click="props.setPrevChunk"
        >
          <a v-bind="{ ...props.aProps, ...props.prevChunkProps }"><strong style="font-size: x-large">{{
            props.texts.prevChunk
          }}</strong></a>
        </li>

        <li :class="props.theme.prev" @click="props.setPrevPage">
          <a v-bind="{ ...props.aProps, ...props.prevProps }"><strong style="font-size: x-large">{{
            props.texts.prevPage
          }}</strong></a>
        </li>

        <li
          v-for="page in props.pages"
          :key="page"
          :class="props.pageClasses(page)"
          v-on="props.pageEvents(page)"
        >
          <a v-bind="props.aProps" :class="props.theme.link"><strong style="font-size: x-large">{{ page }}</strong></a>
        </li>

        <li :class="props.theme.next" @click="props.setNextPage">
          <a v-bind="{ ...props.aProps, ...props.nextProps }"><strong style="font-size: x-large">{{
            props.texts.nextPage
          }}</strong></a>
        </li>

        <li
          v-if="props.hasChunksNav"
          :class="props.theme.nextChunk"
          @click="props.setNextChunk"
        >
          <a v-bind="{ ...props.aProps, ...props.nextChunkProps }"><strong style="font-size: x-large">{{
            props.texts.nextChunk
          }}</strong></a>
        </li>

        <li
          v-if="props.hasEdgeNav"
          :class="props.theme.lastPage"
          @click="props.setLastPage"
        >
          <a v-bind="{ ...props.aProps, ...props.lastPageProps }"><strong style="font-size: x-large">{{
            props.texts.last
          }}</strong></a>
        </li>
      </ul>

      <p v-show="props.hasRecords" :class="props.theme.count">
        <strong style="font-size: x-large">{{ props.count }}</strong>
      </p>
    </nav>
  </div>
</template>
<style scoped>
.pagination > .active > a {
    background-color: lightskyblue;
}
</style>
<script>
export default {
  name: 'MyPagination',
  props: ['props'],
};
</script>
