<template>
    <div class="ibox-content">
        <h1>定型課</h1>
        <ul class="nav nav-tabs">
            <router-link tag="li" :to="{name:'UpdateStylingCard'}" active-class="active"><a>
                <i class="fa fa-th-large"></i>
                <span class="nav-label">定型</span> </a></router-link>
            <router-link tag="li" :to="{name:'StylingReport'}" active-class="active"><a>
                <i class="fa fa-th-large"></i>
                <span class="nav-label">報表</span> </a></router-link>
        </ul>
        <br>
        <router-view></router-view>
    </div>
</template>
<style>
    body{
        background-color:#ff0000;
    }
</style>
<script>
    export default{
        data(){
            return{
            }
        },
        components:{
        }
    }
</script>
