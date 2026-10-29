#!/usr/bin/env bash
# Prints the cloud-init user-data that turns a cloud image into an autologin desktop test VM.
# Ubuntu leaves out the firefox deb (its preinst installs a snap, which fails under cloud-init and
# aborts the whole apt run); Fedora 44 KDE logs in through plasmalogin (not SDDM) and disables plasma-setup,
# which takes the seat ahead of autologin. Both hide their per-user welcome window.
# On Fedora the guest agent's SELinux domain is made permissive, or guest-exec cannot read the system.
# Lab settings for every profile: no sleep, no idle blanking or screen lock (GNOME and KDE), since a
# suspended guest drops its agent channel. `--settings-only` prints just those, for apply.sh.
# usage: user-data.sh <profile> [--settings-only]
set -euo pipefail
profile=$1
settings_only=${2:-}
case "$profile" in
    fedora-gnome)
        groups='wheel, input'
        install='dnf -y install @workstation-product-environment qemu-guest-agent flatpak policycoreutils-python-utils && semanage permissive -a virt_qemu_ga_t'
        login='printf "[daemon]\nAutomaticLoginEnable=True\nAutomaticLogin=tester\n" > /etc/gdm/custom.conf' ;;
    fedora-kde)
        groups='wheel, input'
        install='dnf -y install @kde-desktop-environment qemu-guest-agent flatpak policycoreutils-python-utils && semanage permissive -a virt_qemu_ga_t'
        login='mkdir -p /etc/plasmalogin.conf.d && printf "[Autologin]\nUser=tester\nSession=plasma\n" > /etc/plasmalogin.conf.d/autologin.conf; systemctl disable plasma-setup.service; install -d -o tester -g tester /home/tester/.config /home/tester/.config/autostart; printf "[Desktop Entry]\nHidden=true\n" > /home/tester/.config/autostart/org.kde.plasma-welcome.desktop; chown tester:tester /home/tester/.config/autostart/org.kde.plasma-welcome.desktop' ;;
    ubuntu-gnome)
        groups='sudo, input'
        install='DEBIAN_FRONTEND=noninteractive apt-get -y -o Dpkg::Options::=--force-confold install ubuntu-desktop-minimal firefox- qemu-guest-agent flatpak gnome-software-plugin-flatpak'
        login='printf "[daemon]\nAutomaticLoginEnable=True\nAutomaticLogin=tester\nWaylandEnable=true\n" > /etc/gdm3/custom.conf; install -d -o tester -g tester /home/tester/.config; echo yes > /home/tester/.config/gnome-initial-setup-done; chown tester:tester /home/tester/.config/gnome-initial-setup-done; install -d -o tester -g tester /home/tester/.config/systemd /home/tester/.config/systemd/user; ln -sf /dev/null /home/tester/.config/systemd/user/gnome-initial-setup-upgrade-login.service; chown -h tester:tester /home/tester/.config/systemd/user/gnome-initial-setup-upgrade-login.service' ;;
    *) echo "unknown profile $profile" >&2; exit 2 ;;
esac
settings() {
cat <<'YAML'
write_files:
  - path: /etc/dconf/db/local.d/00-argosy-lab
    content: |
      [org/gnome/desktop/session]
      idle-delay=uint32 0
      [org/gnome/desktop/screensaver]
      lock-enabled=false
      idle-activation-enabled=false
      [org/gnome/settings-daemon/plugins/power]
      sleep-inactive-ac-type='nothing'
      sleep-inactive-battery-type='nothing'
  - path: /etc/xdg/kscreenlockerrc
    content: |
      [Daemon]
      Autolock=false
      LockOnResume=false
  - path: /etc/xdg/powerdevilrc
    content: |
      [AC][Display]
      TurnOffDisplayWhenIdle=false
      DimDisplayWhenIdle=false
      [AC][SuspendAndShutdown]
      AutoSuspendAction=0
YAML
}
settings_runcmd='  - [systemctl, mask, sleep.target, suspend.target, hibernate.target, hybrid-sleep.target]
  - [sh, -c, "mkdir -p /etc/dconf/profile; grep -qs system-db:local /etc/dconf/profile/user || printf \"user-db:user\\nsystem-db:local\\n\" > /etc/dconf/profile/user; dconf update || true"]'
if [ "$settings_only" = --settings-only ]; then
    echo '#cloud-config'
    settings
    printf 'runcmd:\n%s\n' "$settings_runcmd"
    printf 'power_state:\n  mode: poweroff\n  delay: now\n  timeout: 60\n'
    exit 0
fi
cat <<YAML
#cloud-config
hostname: argosy-lab-$profile
users:
  - default
  - name: tester
    gecos: Argosy Tester
    groups: [$groups]
    sudo: "ALL=(ALL) NOPASSWD:ALL"
    shell: /bin/bash
    lock_passwd: true
package_update: true
$(settings)
runcmd:
  - [sh, -c, "$install"]
  - [sh, -c, '$login']
  - [systemctl, enable, qemu-guest-agent]
  - [systemctl, set-default, graphical.target]
  - [flatpak, remote-add, --if-not-exists, flathub, https://dl.flathub.org/repo/flathub.flatpakrepo]
$settings_runcmd
  - [sh, -c, 'echo argosy-lab-ready > /var/lib/argosy-lab-ready']
power_state:
  mode: poweroff
  delay: now
  timeout: 60
YAML
