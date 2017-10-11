<template>
    <div>
        <work-card-list url="/ActiveWorkCard"></work-card-list>
    </div>
</template>
<style>
</style>
<script>
    import axios from 'axios'
    import WorkCardList from './WorkCardList.vue'
    import cardHelper from '../cardHelper'
    export default{
        data(){
            return {
            }
        },
        components: {
            WorkCardList
        }
    }
</script>
