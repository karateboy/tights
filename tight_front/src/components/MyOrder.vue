<template>
    <div>
        <order-list :url="orderUrl()"></order-list>
    </div>
</template>
<style>
    body {
    }


</style>
<script>
    import OrderList from './OrderList.vue'
    import {mapGetters} from 'vuex'

    export default{
        data(){
            return {}
        },
        computed: {
            ...mapGetters(['user'])
        },
        methods: {
            orderUrl(){
                return "/MyActiveOrder/" + this.user._id
            }
        },
        components: {
            OrderList
        }
    }
</script>
