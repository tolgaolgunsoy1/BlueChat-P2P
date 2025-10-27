# Fix Lint Warnings in BlueChat Android Project

## TODO List

- [ ] Remove redundant label from MainActivity in AndroidManifest.xml
- [ ] Update outdated dependencies in libs.versions.toml
- [ ] Fix AsyncTask static field leaks in ChatActivity.java (LoadMessagesTask and SaveMessageTask)
- [ ] Fix Handler leak in ChatActivity.java (make mHandler static)
- [ ] Replace notifyDataSetChanged with more specific notify methods in ChatActivity.java
- [ ] Remove unused color resources from colors.xml
- [ ] Replace "..." with ellipsis character (…) in strings.xml
- [ ] Add autofillHints to EditText in activity_chat.xml
- [ ] Extract hardcoded strings to strings.xml and update layouts (activity_chat.xml, activity_device_list.xml, activity_main.xml, item_message_received.xml, item_message_sent.xml)
- [ ] Change paddingLeft to paddingStart in activity_device_list.xml for RTL support
- [ ] Run lint again to verify all warnings are fixed
