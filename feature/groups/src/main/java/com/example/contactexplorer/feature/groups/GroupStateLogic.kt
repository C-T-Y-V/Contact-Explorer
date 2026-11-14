package com.example.contactexplorer.feature.groups

import com.example.contactexplorer.data.Contact
import com.example.contactexplorer.data.ContactGroup
import com.example.contactexplorer.data.ContactGroupMembership

object GroupStateLogic {
    fun computeGroupCounts(
        groups: List<ContactGroup>,
        memberships: List<ContactGroupMembership>
    ): Map<Long, Int> {
        val countMap = memberships.groupingBy { it.groupId }.eachCount()
        return groups.associate { it.id to (countMap[it.id] ?: 0) }
    }

    fun filterContactsByGroup(
        contacts: List<Contact>,
        memberships: List<ContactGroupMembership>,
        groupId: Long?
    ): List<Contact> {
        if (groupId == null) return contacts
        if (groupId == -1L) {
            val assignedContactIds = memberships.map { it.contactId }.toSet()
            return contacts.filter { it.id !in assignedContactIds }
        }
        val targetContactIds = memberships.filter { it.groupId == groupId }.map { it.contactId }.toSet()
        return contacts.filter { it.id in targetContactIds }
    }

    fun validateMerge(targetGroupId: Long, sourceGroupIds: List<Long>): Boolean {
        return sourceGroupIds.isNotEmpty() && targetGroupId !in sourceGroupIds
    }
}
