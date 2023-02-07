package com.renaisn.reader.data.dao

import androidx.room.*
import com.renaisn.reader.data.entities.RuleSub
import kotlinx.coroutines.flow.Flow

@Dao
interface RuleSubDao {

    @get:Query("select * from ruleSubs order by customOrder")
    val all: List<RuleSub>

    @Query("select * from ruleSubs order by customOrder")
    fun flowAll(): Flow<List<RuleSub>>

    @get:Query("select customOrder from ruleSubs order by customOrder limit 0,1")
    val maxOrder: Int

    @Query("select * from ruleSubs where url = :url")
    fun findByUrl(url: String): RuleSub?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg ruleSub: RuleSub)

    @Delete
    fun delete(vararg ruleSub: RuleSub)

    @Update
    fun update(vararg ruleSub: RuleSub)
}