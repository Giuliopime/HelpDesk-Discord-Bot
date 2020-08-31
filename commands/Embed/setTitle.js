module.exports = {
    // Info
    name: 'settitle',
    description: 'Set a title to for the #help-desk embed',
    aliases: ['stitle'],
    args: true,
    usage: '<title> or <{delete}> to remove',
    cooldown: 5,
    // Basic checks
    guildOnly: true,
    // Command Category
    embed: true,
    // Permissions needed
    perms: ['EMBED_LINKS'],
    async execute(data, member, message, args, index) {
        let text = args.join(' ');
        if(args[0] === '{delete}') text = undefined;
        await message.client.guildSchema.updateOne({guildID: message.guild.id},  { $set:{ ['helpDesks.' + index + '.embedProperties.title']: text } });
        message.client.replyEmbed.setDescription('Title set.\nUse `hd?update` to apply the changes to the #help-desk embed.');
        await message.channel.send(message.client.replyEmbed);
    },
};