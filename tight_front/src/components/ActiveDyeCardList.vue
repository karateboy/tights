<template>
    <div>
        <br>
        <dye-card-list url="/DyeCard"></dye-card-list>
    </div>
</template>
<style scoped>
    body{
    }
</style>
<script>
    import DyeCardList from './DyeCardList.vue'
    import axios from 'axios'
    import cardHelper from '../cardHelper'

    export default{
        data(){
            return{
            }
        },
        components:{
            DyeCardList
        }
    }
</script>
