<template>
    <div class="ibox-content">
        <h1>漂染課</h1>
        <ul class="nav nav-tabs">
            <router-link tag="li" :to="{name:'StartDye'}" active-class="active"><a>
                <i class="fa fa-th-large"></i>
                <span class="nav-label">開始漂染</span> </a></router-link>
            <router-link tag="li" :to="{name:'EndDye'}" active-class="active"><a>
                <i class="fa fa-th-large"></i>
                <span class="nav-label">漂染結束</span> </a></router-link>
            <router-link tag="li" :to="{name:'UpdateDyeCard'}" active-class="active"><a>
                <i class="fa fa-th-large"></i>
                <span class="nav-label">更新漂染卡</span> </a></router-link>
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
